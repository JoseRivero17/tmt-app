package com.uade.tmt.data.remote

import com.uade.tmt.data.remote.parser.InscripcionParser
import com.uade.tmt.domain.model.ResultadoOpciones
import com.uade.tmt.domain.model.ResultadoReserva
import java.io.IOException

/**
 * Inscripción real contra el sitio. Usa la sesión que dejó el login en el CookieJar compartido.
 *
 * Mientras falte la página del formulario con sesión, todo camino que no sea "pide sesión"
 * termina en `ErrorSitio` (ver los TODO(cuenta) de [InscripcionParser] y [enviarReserva]).
 */
class SitioInscripcionRemoteDataSource(
    private val servicio: InscripcionService,
) : InscripcionRemoteDataSource {

    override suspend fun opciones(torneoCodigo: Int): ResultadoOpciones {
        val html = servicio.formulario(torneoCodigo)
        if (InscripcionParser.pideSesion(html)) return ResultadoOpciones.SesionRequerida
        return InscripcionParser.opciones(html)
    }

    override suspend fun reservar(torneoCodigo: Int, categoria: String): ResultadoReserva {
        // Se pide el formulario justo antes de enviar: confirma la sesión y trae los campos
        // ocultos que el sitio pueda exigir en el POST.
        val formulario = servicio.formulario(torneoCodigo)
        if (InscripcionParser.pideSesion(formulario)) return ResultadoReserva.SesionRequerida
        return enviarReserva(formulario, torneoCodigo, categoria)
    }

    /**
     * Envía el formulario de inscripción y lee la respuesta con [InscripcionParser.resultadoReserva].
     *
     * Una IOException DURANTE el envío es [ResultadoReserva.Incierta]: el sitio pudo haber registrado
     * la reserva aunque la respuesta no llegó. Las de antes (al pedir el formulario en [reservar])
     * suben como siempre y el repositorio las informa como `SinConexion`.
     */
    private suspend fun enviarReserva(
        formulario: String,
        torneoCodigo: Int,
        categoria: String,
    ): ResultadoReserva {
        val respuesta = try {
            enviarFormulario(formulario, torneoCodigo, categoria) ?: return ResultadoReserva.ErrorSitio
        } catch (e: IOException) {
            return ResultadoReserva.Incierta
        }
        return InscripcionParser.resultadoReserva(respuesta)
    }

    /**
     * POST del formulario de inscripción; devuelve el HTML de la respuesta.
     *
     * TODO(cuenta): con la página del FORMULARIO guardada (jugadores_inscripciones.asp?torneo_cod=X
     * con sesión iniciada) se ven el `action` del `<form>`, el método y los campos (categoría,
     * aceptación de condiciones, campos ocultos). Con eso: agregar el POST a [InscripcionService] y
     * llamarlo acá. Mientras no exista devuelve `null` (→ `ErrorSitio`). No reintentar el POST
     * automáticamente: si se corta la conexión no se sabe si el sitio lo registró.
     */
    @Suppress("UNUSED_PARAMETER", "RedundantSuspendModifier", "RedundantNullableReturnType")
    private suspend fun enviarFormulario(
        formulario: String,
        torneoCodigo: Int,
        categoria: String,
    ): String? = null
}
