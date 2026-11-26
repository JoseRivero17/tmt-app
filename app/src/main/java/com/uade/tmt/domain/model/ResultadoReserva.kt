package com.uade.tmt.domain.model

/** Cómo terminó un intento de reservar lugar en un torneo (RF02). */
sealed interface ResultadoReserva {
    /** El sitio confirmó el lugar. */
    data object Reservada : ResultadoReserva

    /** El sitio aceptó la inscripción, pero en lista de espera. */
    data object ListaEspera : ResultadoReserva

    /**
     * No se reservó: lo rechazó el sitio o una validación previa (condiciones sin aceptar,
     * categoría deshabilitada). [motivo] se muestra tal cual al jugador.
     */
    data class Rechazada(val motivo: String) : ResultadoReserva

    /** Hace falta iniciar sesión; la UI puede retomar la reserva después del login (criterio 7). */
    data object SesionRequerida : ResultadoReserva

    /** Sin red. La reserva no se encola: hay que reintentarla con conexión (criterio 6). */
    data object SinConexion : ResultadoReserva

    /**
     * Se envió la reserva pero no llegó la respuesta (la conexión se cortó durante el envío): no se
     * sabe si el sitio la registró. No se guarda en el registro local ni se reintenta, porque un
     * reintento podría inscribir dos veces; la UI debe pedir que el jugador lo revise en el sitio.
     */
    data object Incierta : ResultadoReserva

    /** El sitio respondió algo inesperado: no se puede afirmar si la reserva se hizo. */
    data object ErrorSitio : ResultadoReserva
}
