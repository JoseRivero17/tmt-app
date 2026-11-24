package com.uade.tmt.data.remote.parser

import com.uade.tmt.data.remote.LecturaException
import com.uade.tmt.domain.model.CategoriaTorneo
import com.uade.tmt.domain.model.Torneo
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

/** Lee `torneos_ampliar.asp`: filas con una etiqueta ("Divisiones:", "Sede:"…) y su valor. */
object TorneoParser {

    private val HORA = Regex("""(\d{1,2}:\d{2})\s*hs""")
    private val CUPOS = Regex("""Cupos:\s*(\d+)""", RegexOption.IGNORE_CASE)
    private val CODIGO = Regex("""codigo=(\d+)""")

    fun parsear(codigo: Int, html: String): Torneo {
        val pagina = Jsoup.parse(html)
        val escudo = pagina.selectFirst("td > a[href*=clubes_ampliar.asp] > img[src*=escudos]")
        val encabezado = escudo?.closest("tr") ?: throw LecturaException("Torneo $codigo sin encabezado")
        val fecha = pagina.select("strong").firstNotNullOfOrNull { fechaEn(it.ownText()) }
            ?: throw LecturaException("Torneo $codigo sin fecha")
        val divisiones = pagina.valorDe("Divisiones")?.selectFirst("strong")
            ?: throw LecturaException("Torneo $codigo sin divisiones")
        val filaSede = pagina.filaDe("Sede")
        // La dirección está en la fila siguiente: "Calle 123 - LOCALIDAD Provincia".
        val ubicacion = filaSede?.nextElementSibling()?.children()?.last()?.text()?.limpio()

        return Torneo(
            codigo = codigo,
            nombre = encabezado.selectFirst("strong")?.text()?.limpio()
                ?: throw LecturaException("Torneo $codigo sin nombre"),
            fecha = fecha,
            // La localidad ya viene en el calendario; el repositorio conserva esa.
            localidad = "",
            clubCodigo = CODIGO.find(escudo.parent()!!.attr("href"))?.groupValues?.get(1)?.toInt(),
            sede = filaSede?.children()?.last()?.text()?.limpio(),
            direccion = ubicacion,
            precios = pagina.valorDe("Inscripción")?.lineas()?.joinToString("\n")?.ifEmpty { null },
            // Solo el encabezado y las divisiones: el texto libre puede hablar de otras cancelaciones.
            cancelado = indicaCancelacion(encabezado.text() + " " + divisiones.parent()!!.text()),
            inscripcionOnline = divisiones.parent()!!
                .selectFirst("a[href*=jugadores_inscripciones.asp]") != null,
            categorias = divisiones.lineas().map(::categoria),
        )
    }

    /** "7ma división : 3 sets, final 5 sets - 20:30 hs.  3 mesas (Cupos: 18)." */
    private fun categoria(linea: String): CategoriaTorneo {
        val formato = linea.substringAfter(":", "").substringBefore(" - ").limpio()
        return CategoriaTorneo(
            division = nombreDivision(linea.substringBefore(":")),
            detalle = formato.ifEmpty { null },
            hora = HORA.find(linea)?.groupValues?.get(1),
            cupos = CUPOS.find(linea)?.groupValues?.get(1)?.toInt(),
        )
    }

    private fun Document.filaDe(etiqueta: String): Element? =
        select("tr > td:first-child > strong")
            .firstOrNull { it.text().limpio().startsWith("$etiqueta:") }
            ?.closest("tr")

    private fun Document.valorDe(etiqueta: String): Element? = filaDe(etiqueta)?.children()?.getOrNull(1)
}
