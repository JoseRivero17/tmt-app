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
     * Reemplaza los torneos futuros por los que publica el sitio. La lista no trae sede, precios ni
     * cupos: se conservan los que ya se hubieran bajado con el detalle.
     */
    @Transaction
    open suspend fun reemplazarCalendario(
        torneos: List<TorneoEntity>,
        categorias: List<CategoriaTorneoEntity>,
        desde: LocalDate,
    ) {
        val codigos = torneos.map { it.codigo }
        val previos = obtener(codigos).associateBy { it.codigo }
        val categoriasPrevias = obtenerCategorias(codigos).associateBy { it.torneoCodigo to it.division }

        borrarFuturosAusentes(desde, codigos)
        guardar(
            torneos.map { nuevo ->
                val previo = previos[nuevo.codigo]
                nuevo.copy(
                    clubCodigo = nuevo.clubCodigo ?: previo?.clubCodigo,
                    sede = nuevo.sede ?: previo?.sede,
                    direccion = nuevo.direccion ?: previo?.direccion,
                    precios = nuevo.precios ?: previo?.precios,
                )
            },
        )
        borrarCategorias(codigos)
        insertarCategorias(
            categorias.map { nueva ->
                val previa = categoriasPrevias[nueva.torneoCodigo to nueva.division]
                nueva.copy(
                    detalle = nueva.detalle ?: previa?.detalle,
                    cupos = nueva.cupos ?: previa?.cupos,
                )
            },
        )
    }

    /** El detalle no trae la localidad: se conserva la del calendario. */
    @Transaction
    open suspend fun guardarDetalle(torneo: TorneoEntity, categorias: List<CategoriaTorneoEntity>) {
        val previo = obtener(listOf(torneo.codigo)).firstOrNull()
        guardar(listOf(torneo.copy(localidad = torneo.localidad.ifEmpty { previo?.localidad.orEmpty() })))
        borrarCategorias(listOf(torneo.codigo))
        insertarCategorias(categorias)
    }

    @Query("SELECT * FROM torneos WHERE codigo IN (:codigos)")
    protected abstract suspend fun obtener(codigos: List<Int>): List<TorneoEntity>

    @Query("SELECT * FROM categorias_torneo WHERE torneoCodigo IN (:codigos)")
    protected abstract suspend fun obtenerCategorias(codigos: List<Int>): List<CategoriaTorneoEntity>

    @Upsert
    protected abstract suspend fun guardar(torneos: List<TorneoEntity>)

    @Insert
    protected abstract suspend fun insertarCategorias(categorias: List<CategoriaTorneoEntity>)

    @Query("DELETE FROM categorias_torneo WHERE torneoCodigo IN (:codigos)")
    protected abstract suspend fun borrarCategorias(codigos: List<Int>)

    @Query("DELETE FROM torneos WHERE fecha >= :desde AND codigo NOT IN (:vigentes)")
    protected abstract suspend fun borrarFuturosAusentes(desde: LocalDate, vigentes: List<Int>)
}
