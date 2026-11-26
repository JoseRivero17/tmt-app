package com.uade.tmt.domain.model

/**
 * Categoría que el formulario de inscripción de un torneo ofrece al jugador (RF02, criterio 2).
 * Una opción deshabilitada se muestra igual, con el [motivo] por el que no se puede elegir.
 */
data class OpcionInscripcion(
    val categoria: String,
    val horario: String? = null,
    val habilitada: Boolean = true,
    val motivo: String? = null,
)

/** Cómo terminó la consulta de las opciones de inscripción de un torneo. */
sealed interface ResultadoOpciones {
    data class Disponibles(val opciones: List<OpcionInscripcion>) : ResultadoOpciones

    /** El sitio pide iniciar sesión para ver el formulario. */
    data object SesionRequerida : ResultadoOpciones

    data object SinConexion : ResultadoOpciones

    /** El sitio respondió algo inesperado. */
    data object ErrorSitio : ResultadoOpciones
}
