package com.uade.tmt.data.repository

import com.uade.tmt.data.local.dao.InscripcionDao
import com.uade.tmt.data.local.entity.InscripcionEntity
import com.uade.tmt.data.remote.FakeInscripcionRemoteDataSource
import com.uade.tmt.data.remote.InscripcionRemoteDataSource
import com.uade.tmt.data.remote.LecturaException
import com.uade.tmt.data.remote.TorneosFalsos
import com.uade.tmt.data.remote.torneoDePrueba
import com.uade.tmt.domain.model.EstadoInscripcion
import com.uade.tmt.domain.model.Inscripcion
import com.uade.tmt.domain.model.ResultadoOpciones
import com.uade.tmt.domain.model.ResultadoReserva
import com.uade.tmt.domain.repository.Conectividad
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.time.Duration

class ReservaRepositoryImplTest {

    private val ahora = Instant.parse("2026-11-25T15:00:00Z")
    private val reloj = Clock.fixed(ahora, ZoneOffset.UTC)
    private val dao = InscripcionDaoFake()

    private fun repo(
        remoto: InscripcionRemoteDataSource,
        conectado: Boolean = true,
    ) = ReservaRepositoryImpl(remoto, dao, ConectividadFija(conectado), reloj)

    @Test
    fun `sin conexion no consulta el sitio, no encola y no guarda`() = runBlocking {
        val remoto = RemotoFalso { error("No debería consultarse") }
        val repo = repo(remoto, conectado = false)

        assertEquals(ResultadoReserva.SinConexion, repo.reservar(11036, "6ta"))
        assertEquals(ResultadoOpciones.SinConexion, repo.obtenerOpciones(11036))
        assertEquals(0, remoto.llamadas)
        assertTrue(dao.guardadas.isEmpty())
    }

    @Test
    fun `una reserva confirmada queda en el registro local`() = runBlocking {
        val repo = repo(RemotoFalso { ResultadoReserva.Reservada })

        assertEquals(ResultadoReserva.Reservada, repo.reservar(11036, "6ta"))
        assertEquals(
            listOf(InscripcionEntity(11036, "6ta", EstadoInscripcion.RESERVADA, ahora)),
            dao.guardadas,
        )
    }

    @Test
    fun `la lista de espera tambien se guarda`() = runBlocking {
        val repo = repo(RemotoFalso { ResultadoReserva.ListaEspera })

        repo.reservar(11036, "7ma")

        assertEquals(EstadoInscripcion.LISTA_ESPERA, dao.guardadas.single().estado)
    }

    @Test
    fun `rechazo, sesion requerida y error del sitio no se guardan`() = runBlocking {
        val resultados = listOf(
            ResultadoReserva.Rechazada("Sin cupo"),
            ResultadoReserva.SesionRequerida,
            ResultadoReserva.ErrorSitio,
        )
        for (resultado in resultados) {
            assertEquals(resultado, repo(RemotoFalso { resultado }).reservar(11036, "6ta"))
        }
        assertTrue(dao.guardadas.isEmpty())
    }

    @Test
    fun `un corte de red durante la reserva es sin conexion y no se guarda`() = runBlocking {
        for (falla in listOf(UnknownHostException(), SocketTimeoutException())) {
            val repo = repo(RemotoFalso { throw falla })

            assertEquals(ResultadoReserva.SinConexion, repo.reservar(11036, "6ta"))
            assertEquals(ResultadoOpciones.SinConexion, repo.obtenerOpciones(11036))
        }
        assertTrue(dao.guardadas.isEmpty())
    }

    @Test
    fun `una reserva incierta no se guarda ni se reintenta`() = runBlocking {
        val remoto = RemotoFalso { ResultadoReserva.Incierta }

        assertEquals(ResultadoReserva.Incierta, repo(remoto).reservar(11036, "6ta"))
        assertEquals(1, remoto.llamadas)
        assertTrue(dao.guardadas.isEmpty())
    }

    @Test
    fun `respuestas ilegibles o excepciones inesperadas no crashean`() = runBlocking {
        for (falla in listOf(LecturaException("ilegible"), IllegalStateException())) {
            val repo = repo(RemotoFalso { throw falla })

            assertEquals(ResultadoReserva.ErrorSitio, repo.reservar(11036, "6ta"))
            assertEquals(ResultadoOpciones.ErrorSitio, repo.obtenerOpciones(11036))
        }
        assertTrue(dao.guardadas.isEmpty())
    }

    @Test
    fun `si falla Room se informa igual lo que contesto el sitio`() = runBlocking {
        dao.fallar = true
        val repo = repo(RemotoFalso { ResultadoReserva.Reservada })

        assertEquals(ResultadoReserva.Reservada, repo.reservar(11036, "6ta"))
    }

    @Test
    fun `la cancelacion se propaga`() {
        val repo = repo(RemotoFalso { throw CancellationException() })

        assertThrows(CancellationException::class.java) {
            runBlocking { repo.reservar(11036, "6ta") }
        }
    }

    @Test
    fun `con la fuente falsa se reserva y se observa el registro`() = runBlocking {
        val fuente = FakeInscripcionRemoteDataSource(TorneosFalsos(torneoDePrueba(11036)), Duration.ZERO)
        val repo = repo(fuente)

        val opciones = (repo.obtenerOpciones(11036) as ResultadoOpciones.Disponibles).opciones
        assertEquals(listOf("5ta", "6ta", "7ma"), opciones.map { it.categoria })
        assertTrue(repo.reservar(11036, "5ta") is ResultadoReserva.Rechazada)
        assertEquals(ResultadoReserva.Reservada, repo.reservar(11036, "6ta"))
        assertEquals(ResultadoReserva.ListaEspera, repo.reservar(11036, "7ma"))

        assertEquals(
            listOf(
                Inscripcion(11036, "6ta", EstadoInscripcion.RESERVADA, ahora),
                Inscripcion(11036, "7ma", EstadoInscripcion.LISTA_ESPERA, ahora),
            ),
            repo.observarInscripciones().first(),
        )
    }

    private class RemotoFalso(private val respuesta: () -> ResultadoReserva) : InscripcionRemoteDataSource {
        var llamadas = 0

        override suspend fun opciones(torneoCodigo: Int): ResultadoOpciones {
            llamadas++
            respuesta()
            return ResultadoOpciones.Disponibles(emptyList())
        }

        override suspend fun reservar(torneoCodigo: Int, categoria: String): ResultadoReserva {
            llamadas++
            return respuesta()
        }
    }

    private class ConectividadFija(valor: Boolean) : Conectividad {
        override val conectado: Flow<Boolean> = MutableStateFlow(valor)
    }

    private class InscripcionDaoFake : InscripcionDao {
        val guardadas = mutableListOf<InscripcionEntity>()
        var fallar = false

        override fun observarTodas(): Flow<List<InscripcionEntity>> = flowOf(guardadas.toList())

        override suspend fun guardar(inscripcion: InscripcionEntity) {
            if (fallar) throw IllegalStateException("Room no disponible")
            guardadas.removeAll {
                it.torneoCodigo == inscripcion.torneoCodigo && it.categoria == inscripcion.categoria
            }
            guardadas += inscripcion
        }
    }
}
