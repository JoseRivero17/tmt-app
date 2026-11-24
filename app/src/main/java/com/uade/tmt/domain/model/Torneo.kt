package com.uade.tmt.domain.model

import java.time.LocalDate

/** Torneo del circuito. [categorias], [sede] y [direccion] se completan al consultar el detalle. */
data class Torneo(
    val codigo: Int,
    val nombre: String,
    val fecha: LocalDate,
    val localidad: String,
    val club: String? = null,
    val sede: String? = null,
    val direccion: String? = null,
    val cancelado: Boolean = false,
    val inscripcionOnline: Boolean = false,
    val categorias: List<CategoriaTorneo> = emptyList(),
) {
    /** RF02: nunca se ofrece la reserva en un torneo cancelado. */
    val admiteReserva: Boolean get() = inscripcionOnline && !cancelado
}

/** División habilitada en un torneo, con los datos que publica el sitio. */
data class CategoriaTorneo(
    val division: String,
    val detalle: String? = null,
    val hora: String? = null,
    val cupos: Int? = null,
)
