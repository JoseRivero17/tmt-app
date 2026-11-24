package com.uade.tmt.data.repository

import com.uade.tmt.data.local.dao.JugadorDao
import com.uade.tmt.data.local.dao.SyncInfoDao
import com.uade.tmt.data.local.entity.SeguidoEntity
import com.uade.tmt.data.mapper.aDominio
import com.uade.tmt.data.mapper.aEntidad
import com.uade.tmt.data.mapper.normalizado
import com.uade.tmt.data.remote.LecturaException
import com.uade.tmt.data.remote.TmtRemoteDataSource
import com.uade.tmt.domain.model.DIVISIONES
import com.uade.tmt.domain.model.Jugador
import com.uade.tmt.domain.model.Recurso
import com.uade.tmt.domain.model.ResultadoSync
import com.uade.tmt.domain.model.Seguido
import com.uade.tmt.domain.repository.JugadorRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.Instant

class JugadorRepositoryImpl(
    private val dao: JugadorDao,
    private val syncInfo: SyncInfoDao,
    private val remoto: TmtRemoteDataSource,
    private val sincronizador: Sincronizador,
    private val clock: Clock = Clock.systemUTC(),
) : JugadorRepository {

    override fun observarRanking(division: String): Flow<List<Jugador>> =
        dao.observarRanking(division).map { lista -> lista.map { it.aDominio() } }

    override fun observarActualizacionRanking(division: String): Flow<Instant?> =
        syncInfo.observar(Recurso.ranking(division).clave)

    override fun buscar(texto: String): Flow<List<Jugador>> =
        dao.buscar(texto.normalizado()).map { lista -> lista.map { it.aDominio() } }

    override fun observarJugador(codigo: Int): Flow<Jugador?> =
        dao.observar(codigo).map { it?.aDominio() }

    override fun observarSeguidos(): Flow<List<Seguido>> =
        dao.observarSeguidos().map { lista -> lista.map { it.aDominio() } }

    override suspend fun seguir(codigo: Int, esPropio: Boolean) =
        dao.seguir(SeguidoEntity(codigo, esPropio))

    override suspend fun dejarDeSeguir(codigo: Int) = dao.dejarDeSeguir(codigo)

    override suspend fun sincronizarRanking(division: String, forzar: Boolean): ResultadoSync =
        sincronizador.sincronizar(Recurso.ranking(division), forzar) {
            val jugadores = remoto.ranking(division)
            if (jugadores.isEmpty()) throw LecturaException("El ranking de $division llegó vacío")
            val ahora = clock.instant()
            dao.guardar(jugadores.map { it.aEntidad(ahora) })
        }

    /** De a una división, para no cargar al sitio. Las que siguen vigentes no se vuelven a pedir. */
    override suspend fun precargarRanking(): ResultadoSync {
        var resultado: ResultadoSync = ResultadoSync.Vigente
        for (division in DIVISIONES) {
            when (val parcial = sincronizarRanking(division, forzar = false)) {
                is ResultadoSync.Fallo -> return parcial
                ResultadoSync.Actualizado -> resultado = parcial
                ResultadoSync.Vigente -> Unit
            }
        }
        return resultado
    }
}
