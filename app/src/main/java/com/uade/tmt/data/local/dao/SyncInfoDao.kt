package com.uade.tmt.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.uade.tmt.data.local.entity.SyncInfoEntity
import kotlinx.coroutines.flow.Flow
import java.time.Instant

@Dao
interface SyncInfoDao {

    @Query("SELECT actualizadoEn FROM sync_info WHERE recurso = :recurso")
    fun observar(recurso: String): Flow<Instant?>

    @Query("SELECT actualizadoEn FROM sync_info WHERE recurso = :recurso")
    suspend fun obtener(recurso: String): Instant?

    @Upsert
    suspend fun guardar(info: SyncInfoEntity)
}
