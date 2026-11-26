package com.uade.tmt.data.repository

import com.uade.tmt.data.local.dao.ClubDao
import com.uade.tmt.data.local.dao.SyncInfoDao
import com.uade.tmt.data.local.entity.ClubEntity
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

/**
 * RF04. El listado (`clubes_mapaTodos.asp`) trae nombre, localidad y coordenadas de todos los
 * clubes; la ficha de cada uno suma dirección y teléfono. Las dos se guardan en la misma tabla.
 */
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

    override fun observarActualizacionClub(codigo: Int): Flow<Instant?> =
        syncInfo.observar(Recurso.club(codigo).clave)

    override suspend fun sincronizar(forzar: Boolean): ResultadoSync =
        sincronizador.sincronizar(Recurso.CLUBES, forzar) {
            val clubes = remoto.clubes()
            if (clubes.isEmpty()) throw LecturaException("El listado de clubes llegó vacío")
            // El listado no trae dirección ni teléfono: se conservan los de las fichas ya bajadas.
            val fichas = dao.obtenerTodos().associateBy { it.codigo }
            dao.reemplazarTodos(clubes.map { it.aEntidad().conFicha(fichas[it.codigo]) })
        }

    override suspend fun sincronizarClub(codigo: Int, forzar: Boolean): ResultadoSync =
        sincronizador.sincronizar(Recurso.club(codigo), forzar) {
            val ficha = remoto.club(codigo).aEntidad()
            val guardado = dao.obtener(codigo)
            // Si la ficha no trae algo que el listado sí, se conserva lo del listado.
            dao.guardarFicha(
                ficha.copy(
                    localidad = ficha.localidad ?: guardado?.localidad,
                    latitud = ficha.latitud ?: guardado?.latitud,
                    longitud = ficha.longitud ?: guardado?.longitud,
                ),
            )
        }

    private fun ClubEntity.conFicha(ficha: ClubEntity?): ClubEntity = if (ficha == null) {
        this
    } else {
        copy(direccion = ficha.direccion, telefono = ficha.telefono, mail = ficha.mail)
    }
}
