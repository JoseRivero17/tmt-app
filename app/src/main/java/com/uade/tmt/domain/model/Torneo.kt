package com.uade.tmt.domain.model

import java.time.LocalDate

/** Torneo del circuito. La sede, la dirección, los precios y los cupos llegan al consultar el detalle. */
data class Torneo(
    val codigo: Int,
    val nombre: String,
    val fecha: LocalDate,
    val localidad: String,
    val clubCodigo: Int? = null,
    /** Nombre del club organizador, si ya está entre los clubes guardados. */
    val club: String? = null,
    val sede: String? = null,
    val direccion: String? = null,
    val precios: String? = null,
    val cancelado: Boolean = false,
    val inscripcionOnline: Boolean = false,
    val categorias: List<CategoriaTorneo> = emptyList(),
) {
    /** RF02: nunca se ofrece la reserva en un torneo cancelado. */
    val admiteReserva: Boolean get() = inscripcionOnline && !cancelado
}

/** División habilitada en un torneo ("7ma"), con los datos que publica el sitio. */
data class CategoriaTorneo(
    val division: String,
    val detalle: String? = null,
    val hora: String? = null,
    val cupos: Int? = null,
)
