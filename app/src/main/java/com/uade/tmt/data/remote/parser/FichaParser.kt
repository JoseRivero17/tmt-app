package com.uade.tmt.data.remote.parser

import com.uade.tmt.data.mapper.normalizado
import com.uade.tmt.data.remote.LecturaException
import com.uade.tmt.domain.model.Jugador
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

/**
 * Lee `jugadores_ficha.asp`: nombre, código, rating, división, variación y club de un jugador.
 *
 * Sirve para el perfil propio, que casi nunca está entre los primeros 100 de su división.
 * El puesto queda en `null`: la ficha publica la posición en el ranking general, no dentro de la
 * división, y mezclarlas confundiría (un 50° de 5ta figura 812° en su ficha).
 * Los datos personales de la ficha (edad, residencia, foto) no se leen.
 */
object FichaParser {

    private val DIVISION = Regex("""icono_puesto_(\w+)\.""")
    private val CODIGO_CLUB = Regex("""codigo=(\d+)""")
    private val ESCUDO = Regex("""escudos/(\w+)\.""")

    fun parsear(codigo: Int, html: String): Jugador {
        val pagina = Jsoup.parse(html)
        val celdaRating = pagina.celdaEtiquetada("rating:")
            ?: throw LecturaException("La ficha $codigo no tiene rating")
        val valorRating = celdaRating.nextElementSibling()
            ?: throw LecturaException("La ficha $codigo no tiene rating")

        val codigoPagina = pagina.codigo()
        if (codigoPagina != codigo) {
            throw LecturaException("Se pidió la ficha $codigo y llegó la de $codigoPagina")
        }

        val celdaClub = pagina.celdaEtiquetada("club:")?.nextElementSibling()
        val enlaceClub = celdaClub?.selectFirst("a[href*=clubes_ampliar.asp]")

        return Jugador(
            codigo = codigo,
            nombre = pagina.nombre() ?: throw LecturaException("La ficha $codigo no tiene nombre"),
            division = DIVISION.find(valorRating.nextElementSibling()?.html().orEmpty())?.groupValues?.get(1)
                ?: throw LecturaException("La ficha $codigo no tiene división"),
            puesto = null,
            rating = valorRating.text().numero() ?: throw LecturaException("La ficha $codigo no tiene rating"),
            variacion = pagina.celdaEtiquetada("variacion ultimo ranking:")?.nextElementSibling()?.text()?.numero(),
            clubCodigo = enlaceClub?.attr("href")?.let { CODIGO_CLUB.find(it)?.groupValues?.get(1)?.toInt() },
            // Igual que en el ranking: la abreviatura (la del escudo). El nombre sale de los clubes guardados.
            club = celdaClub?.selectFirst("img[src*=escudos/]")?.attr("src")
                ?.let { ESCUDO.find(it)?.groupValues?.get(1) },
        )
    }

    /** Celda cuyo texto es la etiqueta, sin importar tildes ni mayúsculas ("Variación último ranking:"). */
    private fun Document.celdaEtiquetada(etiqueta: String): Element? =
        select("td").firstOrNull { it.children().isEmpty() && it.text().normalizado() == etiqueta }

    /** El código está en un recuadro con la etiqueta "Código" y el número en la celda de al lado. */
    private fun Document.codigo(): Int? =
        celdaEtiquetadaConDiv("codigo")?.nextElementSibling()?.text()?.numero()

    /** El nombre está en la misma fila que el recuadro del código, en la celda anterior. */
    private fun Document.nombre(): String? {
        val recuadro = celdaEtiquetadaConDiv("codigo")?.parents()?.firstOrNull { it.tagName() == "td" }
        return recuadro?.previousElementSibling()?.text()?.limpio()?.ifEmpty { null }
    }

    /** La etiqueta "Código" está dentro de un `<div>`, a diferencia de las de los datos. */
    private fun Document.celdaEtiquetadaConDiv(etiqueta: String): Element? =
        select("td > div").firstOrNull { it.text().normalizado() == etiqueta }?.parent()

    /** "2338" → 2338, "+12" → 12, "-" → null. */
    private fun String.numero(): Int? = limpio().removePrefix("+").toIntOrNull()
}
