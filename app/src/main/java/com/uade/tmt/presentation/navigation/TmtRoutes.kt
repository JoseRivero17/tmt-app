package com.uade.tmt.presentation.navigation

import kotlinx.serialization.Serializable

/** Rutas type-safe de las secciones principales (barra inferior). */
@Serializable
data object Torneos

/** Pantallas dentro de la sección Torneos. */
@Serializable
data object TorneosLista

@Serializable
data class TorneoDetalle(val codigo: Int)

@Serializable
data object Ranking

@Serializable
data object Clubes

@Serializable
data object Perfil
