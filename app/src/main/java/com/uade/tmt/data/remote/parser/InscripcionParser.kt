package com.uade.tmt.data.remote.parser

import com.uade.tmt.data.remote.EstadoAcceso
import com.uade.tmt.data.remote.LecturaException
import com.uade.tmt.domain.model.OpcionInscripcion
import com.uade.tmt.domain.model.ResultadoOpciones
import com.uade.tmt.domain.model.ResultadoReserva
import org.jsoup.Jsoup
import org.jsoup.nodes.Element

/**
 * Páginas de `jugadores_inscripciones.asp`. Se conocen la respuesta SIN sesión
 * (test/resources/html/inscripcion.html) y la página CON sesión que lista las categorías
 * (inscripcionFormulario.html). Falta la respuesta al envío.
 */
object InscripcionParser {

    private const val CAMPO_CATEGORIA = "input[name=categoria]"
    private const val CAMPO_HORARIO = "input[name=horario]"

    /** Las categorías habilitadas tienen este botón; abre el formulario en un popup. */
    private const val BOTON_INSCRIBIRME = "input[type=button][onclick*=jugadores_inscripcionesForm.asp]"

    /** Campos ocultos de cada fila que el popup copia a su formulario antes de enviarlo. */
    private val CAMPOS_FILA = listOf("categoria_cod", "categoria", "torneo_fecha", "torneo_cod", "localidad", "horario")

    /**
     * Lo que el popup agrega: el consentimiento y las dos casillas que el jugador tilda (compartir
     * sus datos con el organizador y el deslinde de responsabilidad). En la app equivalen a
     * "Acepto las condiciones", que [com.uade.tmt.domain.usecase.ReservarLugar] exige antes de enviar.
     */
    private val CAMPOS_ACEPTACION = mapOf("consentimiento" to "1", "chkResp" to "on", "chkDDJJ" to "on")

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
     * Cada categoría es una fila con sus campos ocultos; las que no se pueden elegir muestran el
     * motivo en lugar del botón (por ejemplo, "Tu rating no te habilita a inscribirte…").
     *
     * Lanza [LecturaException] si la página no tiene categorías: no se adivina.
     */
    fun opciones(html: String): ResultadoOpciones {
        val filas = Jsoup.parse(html).select(CAMPO_CATEGORIA).mapNotNull { it.closest("tr") }.distinct()
        if (filas.isEmpty()) throw LecturaException("El formulario de inscripción no tiene categorías")
        return ResultadoOpciones.Disponibles(filas.map(::opcion))
    }

    /**
     * Campos del POST a `jugadores_inscripciones_paso2.asp` para [categoria], tal como los arma el
     * popup del sitio. `null` si la categoría no figura o no está habilitada.
     */
    fun camposEnvio(html: String, categoria: String): Map<String, String>? {
        val fila = Jsoup.parse(html).select(CAMPO_CATEGORIA)
            .firstOrNull { it.attr("value").limpio() == categoria }?.closest("tr") ?: return null
        if (fila.selectFirst(BOTON_INSCRIBIRME) == null) return null
        val campos = CAMPOS_FILA.associateWith { nombre ->
            fila.selectFirst("input[name=$nombre]")?.attr("value")
                ?: throw LecturaException("A la categoría $categoria le falta el campo $nombre")
        }
        return campos + CAMPOS_ACEPTACION
    }

    private fun opcion(fila: Element): OpcionInscripcion {
        val categoria = fila.selectFirst(CAMPO_CATEGORIA)?.attr("value")?.limpio()
            ?.ifEmpty { null } ?: throw LecturaException("Categoría sin nombre")
        val habilitada = fila.selectFirst(BOTON_INSCRIBIRME) != null
        return OpcionInscripcion(
            categoria = categoria,
            horario = fila.selectFirst(CAMPO_HORARIO)?.attr("value")?.limpio()
                ?.removeSuffix("hs.")?.trim()?.ifEmpty { null },
            habilitada = habilitada,
            // El motivo está en la última celda de la fila, donde iría el botón.
            motivo = if (habilitada) null else fila.children().lastOrNull()?.text()?.limpio()?.ifEmpty { null },
        )
    }

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
