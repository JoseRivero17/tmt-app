package com.uade.tmt.domain.repository

import com.uade.tmt.domain.model.ResultadoLogin
import com.uade.tmt.domain.model.SesionJugador
import kotlinx.coroutines.flow.StateFlow

/** Sesión del jugador en el sitio (RF02). La sesión vive solo en memoria (RNF06). */
interface SesionRepository {

    /** Jugador con sesión iniciada, o `null` si no hay sesión. */
    val sesion: StateFlow<SesionJugador?>

    /** Nunca lanza excepciones: cualquier falla se informa como [ResultadoLogin] (RNF02). */
    suspend fun login(codigo: String, clave: String): ResultadoLogin

    /** Borra la sesión y sus cookies. */
    suspend fun logout()
}
