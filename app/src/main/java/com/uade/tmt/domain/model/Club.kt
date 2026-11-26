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

/** Dónde está el club en el mapa, o null si el sitio no lo ubicó. */
val Club.ubicacion: Ubicacion?
    get() = if (latitud != null && longitud != null) Ubicacion(latitud, longitud) else null

/** Un club con su distancia al usuario; null si no se conoce alguna de las dos ubicaciones. */
data class ClubCercano(val club: Club, val distanciaKm: Double?)
