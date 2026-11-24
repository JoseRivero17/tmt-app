package com.uade.tmt.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "clubes")
data class ClubEntity(
    @PrimaryKey val codigo: Int,
    val nombre: String,
    val direccion: String?,
    val localidad: String?,
    val telefono: String?,
    val mail: String?,
    val latitud: Double?,
    val longitud: Double?,
)
