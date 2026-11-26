package com.uade.tmt.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.uade.tmt.data.local.entity.ClubEntity
import kotlinx.coroutines.flow.Flow

@Dao
abstract class ClubDao {

    @Query("SELECT * FROM clubes ORDER BY nombre")
    abstract fun observarTodos(): Flow<List<ClubEntity>>

    @Query("SELECT * FROM clubes WHERE codigo = :codigo")
    abstract fun observar(codigo: Int): Flow<ClubEntity?>

    @Query("SELECT * FROM clubes")
    abstract suspend fun obtenerTodos(): List<ClubEntity>

    @Query("SELECT * FROM clubes WHERE codigo = :codigo")
    abstract suspend fun obtener(codigo: Int): ClubEntity?

    /** Guarda la ficha de un club (RF04, detalle). */
    @Upsert
    abstract suspend fun guardarFicha(club: ClubEntity)

    @Transaction
    open suspend fun reemplazarTodos(clubes: List<ClubEntity>) {
        borrarTodos()
        guardar(clubes)
    }

    @Upsert
    protected abstract suspend fun guardar(clubes: List<ClubEntity>)

    @Query("DELETE FROM clubes")
    protected abstract suspend fun borrarTodos()
}
