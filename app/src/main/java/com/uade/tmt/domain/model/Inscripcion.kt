package com.uade.tmt.domain.model

import java.time.Instant

/** Reserva hecha desde la app. El sitio no publica "mis inscripciones": el registro es local (4.13). */
data class Inscripcion(
    val torneoCodigo: Int,
    val categoria: String,
    val estado: EstadoInscripcion,
    val registradaEn: Instant,
)

enum class EstadoInscripcion { RESERVADA, LISTA_ESPERA, NO_FIGURA }
