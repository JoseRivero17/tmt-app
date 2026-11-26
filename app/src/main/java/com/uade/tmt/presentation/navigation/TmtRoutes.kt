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

/** RF04: pantallas dentro de la sección Clubes. */
@Serializable
data object ClubesLista

/** [distancia] ya formateada ("1,2 km"): la ubicación del usuario no sale de la lista (RNF07). */
@Serializable
data class ClubDetalle(val codigo: Int, val distancia: String? = null)

@Serializable
data object Perfil

/** RF02: pantallas internas (no van en la barra inferior). */
@Serializable
data object Login

@Serializable
data class Reserva(val torneoCodigo: Int)
