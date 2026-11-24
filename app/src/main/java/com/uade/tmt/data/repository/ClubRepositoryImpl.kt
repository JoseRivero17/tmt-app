package com.uade.tmt.data.repository

import com.uade.tmt.data.local.dao.ClubDao
import com.uade.tmt.data.local.dao.SyncInfoDao
import com.uade.tmt.data.mapper.aDominio
import com.uade.tmt.data.mapper.aEntidad
import com.uade.tmt.data.remote.LecturaException
import com.uade.tmt.data.remote.TmtRemoteDataSource
import com.uade.tmt.domain.model.Club
import com.uade.tmt.domain.model.Recurso
import com.uade.tmt.domain.model.ResultadoSync
import com.uade.tmt.domain.repository.ClubRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant

class ClubRepositoryImpl(
    private val dao: ClubDao,
    private val syncInfo: SyncInfoDao,
    private val remoto: TmtRemoteDataSource,
    private val sincronizador: Sincronizador,
) : ClubRepository {

    override fun observarClubes(): Flow<List<Club>> =
        dao.observarTodos().map { lista -> lista.map { it.aDominio() } }

    override fun observarClub(codigo: Int): Flow<Club?> =
        dao.observar(codigo).map { it?.aDominio() }

    override fun observarActualizacion(): Flow<Instant?> =
        syncInfo.observar(Recurso.CLUBES.clave)

    override suspend fun sincronizar(forzar: Boolean): ResultadoSync =
        sincronizador.sincronizar(Recurso.CLUBES, forzar) {
            val clubes = remoto.clubes()
            if (clubes.isEmpty()) throw LecturaException("El listado de clubes llegó vacío")
            dao.reemplazarTodos(clubes.map { it.aEntidad() })
        }
}
