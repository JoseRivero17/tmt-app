package com.uade.tmt.data.remote.parser

import com.uade.tmt.data.mapper.normalizado
import com.uade.tmt.data.remote.EstadoAcceso
import com.uade.tmt.data.remote.LecturaException
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

/**
 * Reconoce qué pantalla de acceso devolvió el sitio. Si la página no tiene la estructura esperada
 * lanza [LecturaException] en lugar de adivinar (RNF04).
 *
 * Solo compara selectores y textos ASCII normalizados: la home declara UTF-8 y las páginas de
 * acceso ISO-8859-1, y una tilde mal decodificada no debe cambiar el resultado.
 *
 * Ojo: el sitio abre `<form>` dentro de `<table>` y el parser HTML5 deja ese form vacío, con los
 * inputs como hermanos. Por eso se buscan los inputs, no `form input`.
 */
object AccesoParser {

    /**
     * Barra azul del encabezado, presente en todas las páginas del sitio. Sin ella la página no se
     * considera del sitio (ni legible ni exitosa).
     * TODO(cuenta): con sesión iniciada el encabezado puede cambiar; validar con una página real.
     */
    private const val BARRA_SUPERIOR = "table[background*=columna_barra2]"
    private const val TITULO_SECCION = "td.TituloSecciones"
    private const val CAJA_MENSAJE = "td[bgcolor=#FFFFCA]"
    private const val CAMPO_CLAVE = "input[name=clave], input[type=password]"
    private const val CAMPO_ACCESO_URL = "input[name=accesoURL]"

    private const val TITULO_ACCESO = "acceso de jugadores"
    private const val MSG_DATOS_INCOMPLETOS = "debes completar todos tus datos"
    private const val MSG_SESION_REQUERIDA = "debes ingresar como usuario"

    fun parsear(html: String): EstadoAcceso {
        val pagina = try {
            Jsoup.parse(html)
        } catch (e: Exception) {
            throw LecturaException("La respuesta de acceso no es HTML", e)
        }
        val barra = pagina.selectFirst(BARRA_SUPERIOR)
            ?: throw LecturaException("La página no tiene la barra superior del sitio")

        if (pagina.select(TITULO_SECCION).any { it.text().normalizado() == TITULO_ACCESO }) {
            return pantallaDeAcceso(pagina)
        }
        if (barra.selectFirst(CAMPO_CLAVE) != null) return EstadoAcceso.SinSesion

        // TODO(cuenta): regla conservadora, sin página real de login exitoso. Validarla y leer el
        // nombre del jugador del encabezado.
        if (pagina.selectFirst(CAMPO_CLAVE) != null || pagina.selectFirst(CAJA_MENSAJE) != null) {
            throw LecturaException("Página con campo de clave o mensaje fuera de la pantalla de acceso")
        }
        return EstadoAcceso.Exito(nombre = null)
    }

    /** Pantalla "ACCESO DE JUGADORES": el mensaje en la caja amarilla dice qué pasó. */
    private fun pantallaDeAcceso(pagina: Document): EstadoAcceso {
        if (pagina.selectFirst(CAMPO_ACCESO_URL) == null) {
            throw LecturaException("Pantalla de acceso sin formulario")
        }
        val mensaje = pagina.selectFirst(CAJA_MENSAJE)?.text()?.normalizado()
        if (mensaje.isNullOrEmpty()) throw LecturaException("Pantalla de acceso sin mensaje")

        return when {
            mensaje.startsWith(MSG_DATOS_INCOMPLETOS) -> EstadoAcceso.DatosIncompletos
            mensaje.startsWith(MSG_SESION_REQUERIDA) -> EstadoAcceso.SesionRequerida
            // TODO(cuenta): confirmar el mensaje real que muestra el sitio con una clave incorrecta.
            else -> EstadoAcceso.CredencialesInvalidas
        }
    }
}
