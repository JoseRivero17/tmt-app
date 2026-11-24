package com.uade.tmt.data.repository

import com.uade.tmt.data.local.dao.SyncInfoDao
import com.uade.tmt.data.local.dao.TorneoDao
import com.uade.tmt.data.mapper.aDominio
import com.uade.tmt.data.mapper.aEntidad
import com.uade.tmt.data.mapper.categoriasAEntidad
import com.uade.tmt.data.remote.LecturaException
import com.uade.tmt.data.remote.TmtRemoteDataSource
import com.uade.tmt.domain.model.Recurso
import com.uade.tmt.domain.model.ResultadoSync
import com.uade.tmt.domain.model.Torneo
import com.uade.tmt.domain.repository.TorneoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.Instant
import java.time.LocalDate

class TorneoRepositoryImpl(
    private val dao: TorneoDao,
    private val syncInfo: SyncInfoDao,
    private val remoto: TmtRemoteDataSource,
    private val sincronizador: Sincronizador,
    private val clock: Clock = Clock.systemDefaultZone(),
) : TorneoRepository {

    override fun observarCalendario(): Flow<List<Torneo>> =
        dao.observarDesde(LocalDate.now(clock)).map { lista -> lista.map { it.aDominio() } }

    override fun observarTorneo(codigo: Int): Flow<Torneo?> =
        dao.observar(codigo).map { it?.aDominio() }

    override fun observarActualizacionCalendario(): Flow<Instant?> =
        syncInfo.observar(Recurso.CALENDARIO.clave)

    override fun observarActualizacionTorneo(codigo: Int): Flow<Instant?> =
        syncInfo.observar(Recurso.torneo(codigo).clave)

    override suspend fun sincronizarCalendario(forzar: Boolean): ResultadoSync =
        sincronizador.sincronizar(Recurso.CALENDARIO, forzar) {
            val torneos = remoto.calendario()
            // Una lista vacía es más probable que sea un cambio en el sitio que un circuito sin torneos:
            // se trata como error para no borrar el calendario guardado (RNF04).
            if (torneos.isEmpty()) throw LecturaException("El calendario llegó vacío")
            dao.reemplazarCalendario(
                torneos = torneos.map { it.aEntidad() },
                categorias = torneos.flatMap { it.categoriasAEntidad() },
                desde = LocalDate.now(clock),
            )
        }

    override suspend fun sincronizarTorneo(codigo: Int, forzar: Boolean): ResultadoSync =
        sincronizador.sincronizar(Recurso.torneo(codigo), forzar) {
            val torneo = remoto.torneo(codigo)
            dao.guardarDetalle(torneo.aEntidad(), torneo.categoriasAEntidad())
        }
}
