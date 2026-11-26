package com.uade.tmt.data.remote

import com.uade.tmt.data.remote.parser.InscripcionParser
import com.uade.tmt.domain.model.ResultadoOpciones
import com.uade.tmt.domain.model.ResultadoReserva
import java.io.IOException

/**
 * Inscripción contra el sitio real. Usa la sesión que dejó el login en el CookieJar compartido.
 *
 * Con [enviarAlSitio] en `false` (lo que usa la app) todo se valida contra el sitio (sesión,
 * categorías y si la del jugador está habilitada, y se arman los campos del envío), pero el POST
 * final no se manda y el resultado es [ResultadoReserva.Reservada]: así se prueba el flujo con una
 * cuenta real sin inscribir a nadie. Pasarlo a `true` recién cuando [InscripcionParser.resultadoReserva]
 * sepa leer la respuesta (ver docs/rf02-pendiente-cuenta.md): si no, una inscripción hecha se vería
 * como un error.
 */
class SitioInscripcionRemoteDataSource(
    private val servicio: InscripcionService,
    private val enviarAlSitio: Boolean = false,
) : InscripcionRemoteDataSource {

    override suspend fun opciones(torneoCodigo: Int): ResultadoOpciones {
        val html = servicio.formulario(torneoCodigo)
        if (InscripcionParser.pideSesion(html)) return ResultadoOpciones.SesionRequerida
        return InscripcionParser.opciones(html)
    }

    override suspend fun reservar(torneoCodigo: Int, categoria: String): ResultadoReserva {
        // Se pide el formulario justo antes de enviar: confirma la sesión y trae los campos
        // ocultos de la categoría, que el sitio exige en el POST.
        val formulario = servicio.formulario(torneoCodigo)
        if (InscripcionParser.pideSesion(formulario)) return ResultadoReserva.SesionRequerida

        val campos = InscripcionParser.camposEnvio(formulario, categoria)
            ?: return ResultadoReserva.Rechazada(motivoNoHabilitada(formulario, categoria))
        if (!enviarAlSitio) return ResultadoReserva.Reservada
        return enviar(campos)
    }

    /**
     * Una IOException DURANTE el envío es [ResultadoReserva.Incierta]: el sitio pudo haber registrado
     * la reserva aunque la respuesta no llegó. No se reintenta el POST: podría inscribir dos veces.
     */
    private suspend fun enviar(campos: Map<String, String>): ResultadoReserva {
        val respuesta = try {
            servicio.enviar(campos)
        } catch (e: IOException) {
            return ResultadoReserva.Incierta
        }
        return InscripcionParser.resultadoReserva(respuesta)
    }

    private fun motivoNoHabilitada(formulario: String, categoria: String): String {
        val opciones = InscripcionParser.opciones(formulario) as? ResultadoOpciones.Disponibles
        return opciones?.opciones?.firstOrNull { it.categoria == categoria }?.motivo
            ?: "La categoría $categoria ya no está disponible para inscribirse."
    }
}
