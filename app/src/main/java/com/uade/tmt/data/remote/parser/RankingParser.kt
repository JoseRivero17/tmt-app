package com.uade.tmt.data.remote.parser

import com.uade.tmt.data.remote.LecturaException
import com.uade.tmt.domain.model.Jugador
import org.jsoup.Jsoup
import org.jsoup.nodes.Element

/**
 * Lee `ranking.asp`: una fila de 9 celdas por jugador (división, puesto, código, nombre, rating,
 * variación, club, provincia y último torneo).
 */
object RankingParser {

    private const val CELDAS = 9
    private const val ENLACE_FICHA = "td > a[href*=jugadores_ficha.asp], td > div > a[href*=jugadores_ficha.asp]"
    private val DIVISION = Regex("""icono_puesto_(\w+)\.""")
    private val CODIGO = Regex("""codigo=(\d+)""")

    /** El robots.txt del sitio pide no acceder a estas fichas: no se guardan. */
    private val EXCLUIDOS = setOf(5341, 5149)

    fun parsear(html: String): List<Jugador> {
        val filas = Jsoup.parse(html).select("tr:has($ENLACE_FICHA)").filter { it.children().size == CELDAS }
        if (filas.isEmpty()) throw LecturaException("El ranking no tiene jugadores reconocibles")
        return filas.map(::jugador).filterNot { it.codigo in EXCLUIDOS }
    }

    private fun jugador(fila: Element): Jugador {
        val celdas = fila.children()
        val codigo = celdas[2].text().numero() ?: throw LecturaException("Jugador sin código")
        return Jugador(
            codigo = codigo,
            nombre = celdas[3].text().limpio(),
            division = DIVISION.find(celdas[0].html())?.groupValues?.get(1)
                ?: throw LecturaException("Jugador $codigo sin división"),
            puesto = celdas[1].text().numero(),
            rating = celdas[4].text().numero() ?: throw LecturaException("Jugador $codigo sin rating"),
            variacion = celdas[5].text().numero(),
            clubCodigo = CODIGO.find(celdas[6].html())?.groupValues?.get(1)?.toInt(),
            club = celdas[6].text().limpio().ifEmpty { null },
            provincia = celdas[7].text().limpio().ifEmpty { null },
        )
    }

    /** "12°" → 12, "-8" → -8, "" → null. */
    private fun String.numero(): Int? = limpio().removeSuffix("°").removePrefix("+").trim().toIntOrNull()
}
