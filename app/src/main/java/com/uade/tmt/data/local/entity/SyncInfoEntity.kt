package com.uade.tmt.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

/** Última actualización exitosa de cada recurso (RNF01, RNF05). */
@Entity(tableName = "sync_info")
data class SyncInfoEntity(
    @PrimaryKey val recurso: String,
    val actualizadoEn: Instant,
)
