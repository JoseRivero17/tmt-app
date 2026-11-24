package com.uade.tmt.presentation.torneos

import com.uade.tmt.domain.model.FiltrosTorneo
import com.uade.tmt.domain.model.MotivoFallo
import com.uade.tmt.domain.model.Torneo
import java.time.Instant
import java.time.LocalDate

sealed interface TorneosUiState {

    /** Primer uso: todavía no hay nada guardado. */
    data object Cargando : TorneosUiState

    /** No hay datos guardados y no se pudieron bajar. */
    data class Error(val motivo: MotivoFallo) : TorneosUiState

    /** Hay datos guardados. Puede estar actualizándose o haber fallado la última actualización. */
    data class Contenido(
        val dias: List<DiaTorneos>,
        /** Cantidad de torneos guardados, sin contar los filtros. */
        val total: Int,
        val filtros: FiltrosTorneo,
        val opciones: OpcionesFiltro,
        val actualizadoEn: Instant,
        val actualizando: Boolean,
        val fallo: MotivoFallo?,
        val sinConexion: Boolean,
    ) : TorneosUiState
}

data class DiaTorneos(val fecha: LocalDate, val torneos: List<Torneo>)

/** Valores disponibles para filtrar, según los torneos guardados. */
data class OpcionesFiltro(val divisiones: List<String>, val localidades: List<String>)
