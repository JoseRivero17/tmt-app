package com.uade.tmt.domain.model

import java.time.Instant

/** Jugador tal como figura en el ranking del circuito. */
data class Jugador(
    val codigo: Int,
    val nombre: String,
    val division: String,
    /** Puesto dentro de su división. */
    val puesto: Int?,
    val rating: Int?,
    val variacion: Int? = null,
    val clubCodigo: Int? = null,
    /** Nombre del club si está entre los guardados; si no, la abreviatura del ranking. */
    val club: String? = null,
    val provincia: String? = null,
    /** RF03: la ficha indica de cuándo es el dato. Lo completa la capa de datos al guardar. */
    val actualizadoEn: Instant? = null,
)

/** Jugador marcado por el usuario. [esPropio] distingue el perfil de quien usa la app. */
data class Seguido(
    val jugador: Jugador,
    val esPropio: Boolean,
)
