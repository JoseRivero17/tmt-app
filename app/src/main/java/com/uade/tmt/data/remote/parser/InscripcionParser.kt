package com.uade.tmt.data.remote.parser

import com.uade.tmt.data.remote.EstadoAcceso
import com.uade.tmt.data.remote.LecturaException
import com.uade.tmt.domain.model.ResultadoOpciones
import com.uade.tmt.domain.model.ResultadoReserva

/**
 * Páginas de `jugadores_inscripciones.asp`. Hoy solo se conoce la respuesta SIN sesión
 * (test/resources/html/inscripcion.html); el formulario y la respuesta al envío faltan.
 */
object InscripcionParser {

    /**
     * `true` si el sitio devolvió una pantalla que pide iniciar sesión. Reutiliza [AccesoParser]:
     * cualquier estado que no sea [EstadoAcceso.Exito] significa que no hay sesión válida.
     *
     * Si [AccesoParser] no entiende la página no se concluye nada (`false`): el formulario con
     * sesión podría tener una caja de mensaje que [AccesoParser] rechaza, y decidir si es legible
     * le toca a [opciones] / [resultadoReserva].
     */
    fun pideSesion(html: String): Boolean = try {
        AccesoParser.parsear(html) !is EstadoAcceso.Exito
    } catch (e: LecturaException) {
        false
    }

    /**
     * Opciones del formulario de inscripción (categorías, horarios y cuáles no se pueden elegir).
     *
     * TODO(cuenta): implementar con la página del FORMULARIO guardada con sesión iniciada:
     * `jugadores_inscripciones.asp?torneo_cod=<un torneo con inscripción abierta>`, como
     * test/resources/html/inscripcionFormulario.html. Hasta entonces el sitio real no es legible.
     */
    @Suppress("UNUSED_PARAMETER")
    fun opciones(html: String): ResultadoOpciones = ResultadoOpciones.ErrorSitio

    /**
     * Qué contestó el sitio al enviar el formulario: reservada, lista de espera o rechazo con motivo.
     *
     * TODO(cuenta): implementar con la página de RESPUESTA al envío (el HTML que llega tras el POST),
     * guardada como test/resources/html/inscripcionRespuesta.html; idealmente una de lugar
     * confirmado, una de lista de espera y una de rechazo.
     */
    @Suppress("UNUSED_PARAMETER")
    fun resultadoReserva(html: String): ResultadoReserva = ResultadoReserva.ErrorSitio
}
