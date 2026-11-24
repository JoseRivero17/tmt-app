package com.uade.tmt.domain.model

import java.time.Duration

/**
 * Recurso remoto que se guarda localmente, con la antigüedad a partir de la cual vale la pena
 * volver a pedirlo (RNF05). La actualización manual ignora ese intervalo.
 */
data class Recurso(val clave: String, val intervaloMinimo: Duration) {
    companion object {
        val CALENDARIO = Recurso("calendario", Duration.ofMinutes(30))
        val CLUBES = Recurso("clubes", Duration.ofDays(7))

        fun torneo(codigo: Int) = Recurso("torneo:$codigo", Duration.ofMinutes(30))

        /** El ranking se pide por división: la página sin filtro tarda más que el timeout de RNF03. */
        fun ranking(division: String) = Recurso("ranking:$division", Duration.ofDays(1))
    }
}

/** Resultado de intentar actualizar un recurso. En todos los casos lo guardado sigue disponible. */
sealed interface ResultadoSync {
    /** Se bajaron datos nuevos y se guardaron. */
    data object Actualizado : ResultadoSync

    /** Lo guardado todavía está vigente: no se consultó el sitio. */
    data object Vigente : ResultadoSync

    data class Fallo(val motivo: MotivoFallo) : ResultadoSync
}

enum class MotivoFallo {
    SIN_CONEXION,
    TIMEOUT,

    /** El sitio respondió con un error HTTP. */
    SERVIDOR,

    /** El sitio respondió, pero el HTML no tiene la estructura esperada (RNF04). */
    LECTURA,
}
