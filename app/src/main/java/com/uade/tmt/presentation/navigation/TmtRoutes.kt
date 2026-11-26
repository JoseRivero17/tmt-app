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

/** RF02: pantallas internas (no van en la barra inferior). */
@Serializable
data object Login

@Serializable
data class Reserva(val torneoCodigo: Int)

/** Pantallas dentro de la sección Jugadores (RF03). */
@Serializable
data object RankingLista

@Serializable
data class JugadorFicha(val codigo: Int)
