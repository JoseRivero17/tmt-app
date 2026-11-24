package com.uade.tmt.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.uade.tmt.data.local.entity.CategoriaTorneoEntity
import com.uade.tmt.data.local.entity.TorneoConCategorias
import com.uade.tmt.data.local.entity.TorneoEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
abstract class TorneoDao {

    @Transaction
    @Query("SELECT * FROM torneos WHERE fecha >= :desde ORDER BY fecha, nombre")
    abstract fun observarDesde(desde: LocalDate): Flow<List<TorneoConCategorias>>

    @Transaction
    @Query("SELECT * FROM torneos WHERE codigo = :codigo")
    abstract fun observar(codigo: Int): Flow<TorneoConCategorias?>

    /**
     * Reemplaza los torneos futuros por los que publica el sitio. La lista no trae sede ni
     * dirección: se conservan las que ya se hubieran bajado con el detalle.
     */
    @Transaction
    open suspend fun reemplazarCalendario(torneos: List<TorneoEntity>, desde: LocalDate) {
        val guardados = obtener(torneos.map { it.codigo }).associateBy { it.codigo }
        borrarFuturosAusentes(desde, torneos.map { it.codigo })
        guardar(
            torneos.map { nuevo ->
                val previo = guardados[nuevo.codigo]
                nuevo.copy(
                    club = nuevo.club ?: previo?.club,
                    sede = nuevo.sede ?: previo?.sede,
                    direccion = nuevo.direccion ?: previo?.direccion,
                )
            },
        )
    }

    @Transaction
    open suspend fun guardarDetalle(torneo: TorneoEntity, categorias: List<CategoriaTorneoEntity>) {
        guardar(listOf(torneo))
        borrarCategorias(torneo.codigo)
        insertarCategorias(categorias)
    }

    @Query("SELECT * FROM torneos WHERE codigo IN (:codigos)")
    protected abstract suspend fun obtener(codigos: List<Int>): List<TorneoEntity>

    @Upsert
    protected abstract suspend fun guardar(torneos: List<TorneoEntity>)

    @Insert
    protected abstract suspend fun insertarCategorias(categorias: List<CategoriaTorneoEntity>)

    @Query("DELETE FROM categorias_torneo WHERE torneoCodigo = :codigo")
    protected abstract suspend fun borrarCategorias(codigo: Int)

    @Query("DELETE FROM torneos WHERE fecha >= :desde AND codigo NOT IN (:vigentes)")
    protected abstract suspend fun borrarFuturosAusentes(desde: LocalDate, vigentes: List<Int>)
}
