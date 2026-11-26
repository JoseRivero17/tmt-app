package com.uade.tmt.data.remote.parser

import com.uade.tmt.data.remote.LecturaException
import com.uade.tmt.domain.model.Club
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

/**
 * Lee `clubes_ampliar.asp?codigo=N`, la ficha de un club: dirección, localidad, teléfono de
 * contacto y la ubicación del mapa (RF04: detalle del club y "Cómo llegar").
 */
object ClubParser {

    /** `var lat = -34.796361; var lon =  -58.386436;` en el script del mapa. */
    private val UBICACION = Regex("""var\s+lat\s*=\s*(-?\d+(?:\.\d+)?)\s*;?\s*var\s+lon\s*=\s*(-?\d+(?:\.\d+)?)""")
    private val TELEFONO = Regex("""\d[\d\s()/-]{5,}\d""")

    fun parsear(codigo: Int, html: String): Club {
        val pagina = Jsoup.parse(html)
        val direccion = valorDe(pagina, "Dirección:")
            ?: throw LecturaException("La ficha del club $codigo no tiene dirección")
        val nombre = pagina.selectFirst("td[style*=border-right]")?.text()?.limpio()
            ?.ifEmpty { null }
            ?: throw LecturaException("La ficha del club $codigo no tiene nombre")

        // La fila siguiente a la dirección es "ADROGUE - Pcia. de Gran Buenos Aires".
        val localidad = filaSiguiente(pagina, "Dirección:")?.substringBefore(" - ")?.aTitulo()
        // La fila siguiente al contacto es "1121591685 | mail": el mail está ofuscado por el sitio.
        val telefono = filaSiguiente(pagina, "Contacto:")
            ?.substringBefore("|")
            ?.let { TELEFONO.find(it)?.value?.limpio() }
        val ubicacion = UBICACION.find(html)?.destructured?.let { (lat, lon) ->
            ClubesParser.coordenadas("'$lat, $lon'")
        }

        return Club(
            codigo = codigo,
            nombre = nombre,
            direccion = direccion.ifEmpty { null },
            localidad = localidad,
            telefono = telefono,
            latitud = ubicacion?.first,
            longitud = ubicacion?.second,
        )
    }

    /** Celda de etiqueta ("Dirección:") de la tabla de datos de la ficha. */
    private fun etiqueta(pagina: Document, texto: String): Element? =
        pagina.select("td").firstOrNull { it.ownText().limpio() == texto }

    private fun valorDe(pagina: Document, texto: String): String? =
        etiqueta(pagina, texto)?.nextElementSibling()?.text()?.limpio()

    private fun filaSiguiente(pagina: Document, texto: String): String? =
        etiqueta(pagina, texto)?.parent()?.nextElementSibling()
            ?.select("td")?.getOrNull(1)?.text()?.limpio()?.ifEmpty { null }
}
