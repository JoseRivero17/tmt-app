package com.uade.tmt.domain.model

/**
 * Jugador con sesión iniciada en el sitio (RF02). Vive solo en memoria: no se guarda en disco
 * y nunca incluye la clave (RNF06).
 *
 * No es [Jugador]: ese modelo es el del ranking (RF03).
 */
data class SesionJugador(
    val codigo: String,
    val nombre: String? = null,
)

/** Cómo terminó un intento de login. */
sealed interface ResultadoLogin {
    data class Ok(val sesion: SesionJugador) : ResultadoLogin

    /** Falta el código o la clave. */
    data object DatosIncompletos : ResultadoLogin

    /** El sitio rechazó el código o la clave. */
    data object CredencialesInvalidas : ResultadoLogin

    data object SinConexion : ResultadoLogin

    /** El sitio respondió algo inesperado: no se puede afirmar si el login funcionó. */
    data object ErrorSitio : ResultadoLogin
}
