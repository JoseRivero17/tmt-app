package com.uade.tmt.presentation.clubes

import com.uade.tmt.domain.model.ClubCercano
import com.uade.tmt.domain.model.MotivoFallo
import java.time.Instant

/** Qué decidió el usuario sobre la ubicación (RF04, RNF07). */
enum class AccesoUbicacion {
    /** Todavía no eligió: se muestra la explicación previa (G1). */
    SIN_DECIDIR,
    CONCEDIDO,

    /** Rechazó el permiso: la función sigue con la búsqueda por localidad (G4, criterio 2). */
    RECHAZADO,

    /** Eligió "Buscar por localidad" sin dar el permiso. */
    SIN_UBICACION,
}

sealed interface ClubesUiState {

    /** G1: explica para qué se usa la ubicación antes de mostrar el diálogo del sistema. */
    data object PedirPermiso : ClubesUiState

    /** Primer uso: todavía no hay clubes guardados. */
    data object Cargando : ClubesUiState

    /** No hay clubes guardados y no se pudieron bajar. */
    data class Error(val motivo: MotivoFallo) : ClubesUiState

    /** Hay clubes guardados (G3, G4, G6). */
    data class Contenido(
        val clubes: List<ClubCercano>,
        val busqueda: String,
        val acceso: AccesoUbicacion,
        /** Se está leyendo la ubicación del teléfono. */
        val ubicando: Boolean,
        /** Hay permiso pero el teléfono no dio una ubicación (por ejemplo, ubicación desactivada). */
        val sinUbicacion: Boolean,
        val actualizadoEn: Instant,
        val actualizando: Boolean,
        val fallo: MotivoFallo?,
        val sinConexion: Boolean,
    ) : ClubesUiState {
        val ordenadosPorDistancia: Boolean get() = clubes.any { it.distanciaKm != null }
    }
}
