package com.uade.tmt.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.uade.tmt.data.local.entity.InscripcionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InscripcionDao {

    @Query("SELECT * FROM inscripciones ORDER BY registradaEn DESC")
    fun observarTodas(): Flow<List<InscripcionEntity>>

    @Upsert
    suspend fun guardar(inscripcion: InscripcionEntity)
}
