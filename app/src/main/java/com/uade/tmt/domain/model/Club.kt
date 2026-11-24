package com.uade.tmt.domain.model

data class Club(
    val codigo: Int,
    val nombre: String,
    val direccion: String? = null,
    val localidad: String? = null,
    val telefono: String? = null,
    val mail: String? = null,
    val latitud: Double? = null,
    val longitud: Double? = null,
)
