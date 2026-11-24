package com.uade.tmt.data.local.entity

import androidx.room.Entity
import com.uade.tmt.domain.model.EstadoInscripcion
import java.time.Instant

/** Sin clave foránea a propósito: la reserva se conserva aunque el torneo deje el calendario. */
@Entity(tableName = "inscripciones", primaryKeys = ["torneoCodigo", "categoria"])
data class InscripcionEntity(
    val torneoCodigo: Int,
    val categoria: String,
    val estado: EstadoInscripcion,
    val registradaEn: Instant,
)
