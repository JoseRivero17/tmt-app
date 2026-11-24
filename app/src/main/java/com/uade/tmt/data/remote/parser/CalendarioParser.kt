package com.uade.tmt.data.remote.parser

import com.uade.tmt.data.remote.LecturaException
import com.uade.tmt.domain.model.CategoriaTorneo
import com.uade.tmt.domain.model.Torneo
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.time.LocalDate

/**
 * Lee `calendario.asp`: un encabezado por día y debajo una fila por torneo.
 * Si falta algo esencial lanza [LecturaException] en lugar de devolver datos a medias (RNF04).
 */
object CalendarioParser {

    private const val ENLACE_TORNEO = "a.linkNaranja[href*=torneos_ampliar.asp]"
    private val CODIGO = Regex("""codigo=(\d+)""")
    private val CATEGORIA = Regex("""^(.+?)\s*-\s*(\d{1,2}:\d{2})""")

    fun parsear(html: String): List<Torneo> {
        val torneos = mutableListOf<Torneo>()
        var fecha: LocalDate? = null

        // select devuelve los elementos en el orden de la página: el último día leído es el del torneo.
        for (elemento in Jsoup.parse(html).select("strong, $ENLACE_TORNEO")) {
            if (elemento.tagName() == "a") {
                torneos += torneo(elemento, fecha ?: throw LecturaException("Torneo sin fecha"))
            } else {
                fechaEn(elemento.ownText())?.let { fecha = it }
            }
        }
        if (torneos.isEmpty()) throw LecturaException("El calendario no tiene torneos reconocibles")
        return torneos
    }

    private fun torneo(enlace: Element, fecha: LocalDate): Torneo {
        val codigo = CODIGO.find(enlace.attr("href"))?.groupValues?.get(1)?.toInt()
            ?: throw LecturaException("Torneo sin código")
        val fila = enlace.closest("tr") ?: throw LecturaException("Torneo $codigo fuera de la tabla")
        val localidad = fila.select("u").firstOrNull { it.text().startsWith("Localidad") }
            ?.parent()?.ownText()
            ?: throw LecturaException("Torneo $codigo sin localidad")

        return Torneo(
            codigo = codigo,
            nombre = enlace.text().limpio(),
            fecha = fecha,
            localidad = localidad.aTitulo(),
            cancelado = indicaCancelacion(fila.text()),
            inscripcionOnline = fila.selectFirst("a[href*=jugadores_inscripciones.asp]") != null,
            categorias = fila.children().last()?.selectFirst("strong")?.lineas().orEmpty().mapNotNull(::categoria),
        )
    }

    private fun categoria(linea: String): CategoriaTorneo? {
        val (division, hora) = CATEGORIA.find(linea)?.destructured ?: return null
        return CategoriaTorneo(division = nombreDivision(division), hora = hora)
    }
}
