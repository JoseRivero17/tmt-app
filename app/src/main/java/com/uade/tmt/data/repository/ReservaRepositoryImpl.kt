package com.uade.tmt.data.repository

import com.uade.tmt.data.local.dao.InscripcionDao
import com.uade.tmt.data.local.entity.InscripcionEntity
import com.uade.tmt.data.mapper.aDominio
import com.uade.tmt.data.remote.InscripcionRemoteDataSource
import com.uade.tmt.domain.model.EstadoInscripcion
import com.uade.tmt.domain.model.Inscripcion
import com.uade.tmt.domain.model.ResultadoOpciones
import com.uade.tmt.domain.model.ResultadoReserva
import com.uade.tmt.domain.repository.Conectividad
import com.uade.tmt.domain.repository.ReservaRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.time.Clock

/**
 * RF02. La reserva es siempre online: sin conexión devuelve `SinConexion` sin tocar el sitio ni
 * encolar nada (criterio 6). Solo lo que el sitio aceptó (reservada o lista de espera) queda en el
 * registro local. Ninguna excepción escapa, salvo [CancellationException] (RNF02, RNF04).
 */
class ReservaRepositoryImpl(
    private val remoto: InscripcionRemoteDataSource,
    private val dao: InscripcionDao,
    private val conectividad: Conectividad,
    private val reloj: Clock = Clock.systemUTC(),
) : ReservaRepository {

    override suspend fun obtenerOpciones(torneoCodigo: Int): ResultadoOpciones {
        if (!hayConexion()) return ResultadoOpciones.SinConexion
        return try {
            remoto.opciones(torneoCodigo)
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            ResultadoOpciones.SinConexion
        } catch (e: Exception) {
            // HttpException, LecturaException o cualquier sorpresa del sitio: nunca crashear.
            ResultadoOpciones.ErrorSitio
        }
    }

    override suspend fun reservar(torneoCodigo: Int, categoria: String): ResultadoReserva {
        if (!hayConexion()) return ResultadoReserva.SinConexion
        val resultado = try {
            remoto.reservar(torneoCodigo, categoria)
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            ResultadoReserva.SinConexion
        } catch (e: Exception) {
            ResultadoReserva.ErrorSitio
        }

        val estado = when (resultado) {
            ResultadoReserva.Reservada -> EstadoInscripcion.RESERVADA
            ResultadoReserva.ListaEspera -> EstadoInscripcion.LISTA_ESPERA
            // Incierta: no se guarda ni se reintenta (un reintento podría inscribir dos veces).
            else -> return resultado
        }
        registrar(torneoCodigo, categoria, estado)
        return resultado
    }

    override fun observarInscripciones(): Flow<List<Inscripcion>> =
        dao.observarTodas().map { lista -> lista.map { it.aDominio() } }

    override suspend fun olvidarInscripciones() = dao.borrarTodas()

    /** Si Room falla, la reserva en el sitio igual se hizo: se informa el resultado del sitio. */
    private suspend fun registrar(torneoCodigo: Int, categoria: String, estado: EstadoInscripcion) {
        try {
            dao.guardar(InscripcionEntity(torneoCodigo, categoria, estado, reloj.instant()))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Sin registro local: el jugador puede verla igual en el sitio.
        }
    }

    private suspend fun hayConexion(): Boolean = try {
        conectividad.conectado.first()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        // Si no se puede consultar el estado se intenta igual: la red dirá si hay conexión.
        true
    }
}
