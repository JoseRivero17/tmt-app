package com.uade.tmt.data.remote.parser

import com.uade.tmt.data.remote.LecturaException
import com.uade.tmt.domain.model.Club
import org.jsoup.Jsoup
import org.jsoup.nodes.Element

/**
 * Lee `clubes_mapaTodos.asp`: un bloque por club con nombre, "LOCALIDAD - Provincia" y el enlace
 * "Ubicar en mapa", que trae las coordenadas (RF04: con eso se ordena por cercanía en el teléfono).
 *
 * La misma página lista asociaciones; se descartan porque enlazan a `asociaciones_ampliar.asp`
 * (y sus códigos se pisan con los de los clubes).
 */
object ClubesParser {

    private const val ENLACE_CLUB = "a.linkNaranja[href*=clubes_ampliar.asp]"
    private val CODIGO = Regex("""codigo=(\d+)""")

    /**
     * `goto('#mapa', map,'-34.796361, -58.386436')`. Algunos clubes traen además el zoom del mapa
     * (`'-34.77,-58.63,17'`): se ignora.
     */
    private val COORDENADAS = Regex("""'\s*(-?\d+(?:\.\d+)?)\s*,\s*(-?\d+(?:\.\d+)?)\s*(?:,[^']*)?'""")

    fun parsear(html: String): List<Club> {
        val clubes = Jsoup.parse(html).select(ENLACE_CLUB).map(::club).distinctBy { it.codigo }
        if (clubes.isEmpty()) throw LecturaException("El listado no tiene clubes reconocibles")
        return clubes
    }

    private fun club(enlace: Element): Club {
        val codigo = CODIGO.find(enlace.attr("href"))?.groupValues?.get(1)?.toInt()
            ?: throw LecturaException("Club sin código")
        val nombre = enlace.text().limpio().ifEmpty { throw LecturaException("Club $codigo sin nombre") }
        val bloque = enlace.closest("table") ?: throw LecturaException("Club $codigo fuera de la tabla")

        // La celda sin enlaces del bloque es "ADROGUE - Gran Buenos Aires".
        val ubicacion = bloque.select("td")
            .firstOrNull { it.children().isEmpty() && it.text().contains(" - ") }
            ?.text()
        val (latitud, longitud) = bloque.selectFirst("a[onclick*=goto]")
            ?.let { coordenadas(it.attr("onclick")) }
            ?: (null to null)

        return Club(
            codigo = codigo,
            nombre = nombre,
            localidad = ubicacion?.substringBefore(" - ")?.aTitulo(),
            latitud = latitud,
            longitud = longitud,
        )
    }

    /** Un club sin ubicar en el mapa queda sin coordenadas: se lista al final, sin distancia. */
    internal fun coordenadas(texto: String): Pair<Double, Double>? {
        val (lat, lon) = COORDENADAS.find(texto)?.destructured ?: return null
        val latitud = lat.toDouble()
        val longitud = lon.toDouble()
        val validas = latitud in -90.0..90.0 && longitud in -180.0..180.0 && !(latitud == 0.0 && longitud == 0.0)
        return if (validas) latitud to longitud else null
    }
}
