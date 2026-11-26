package com.uade.tmt.data.remote.parser

import com.uade.tmt.data.remote.LecturaException
import com.uade.tmt.domain.model.Jugador
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class RankingParserTest {

    private val html = html("ranking.html")

    @Test
    fun `lee todos los datos de un jugador`() {
        assertEquals(
            Jugador(
                codigo = 1001,
                nombre = "Muñoz, Ana",
                division = "5ta",
                puesto = 1,
                rating = 1500,
                variacion = -12,
                clubCodigo = 90,
                club = "CLU",
                provincia = "BUE",
            ),
            RankingParser.parsear(html).first(),
        )
    }

    @Test
    fun `los datos opcionales pueden faltar`() {
        val sinOpcionales = RankingParser.parsear(html)[1]

        assertEquals(Jugador(1002, "Pérez, Juan Cruz", "5ta", 2, 1480, clubCodigo = 0), sinOpcionales)
    }

    @Test
    fun `ignora los enlaces a fichas que no son filas del ranking`() {
        assertEquals(listOf(1001, 1002), RankingParser.parsear(html).map { it.codigo })
    }

    @Test
    fun `no guarda los jugadores que el sitio pide no consultar`() {
        assertEquals(listOf(1002), RankingParser.parsear(html.replace("1001", "5341")).map { it.codigo })
    }

    @Test
    fun `si cambia la estructura falla`() {
        listOf(
            html.replace("jugadores_ficha.asp", "jugador.asp"),
            html.replace("icono_puesto_5ta", "icono"),
            html.replace(">1500<", ">s/d<"),
            "<html><body>Mantenimiento</body></html>",
        ).forEach { alterado ->
            assertThrows(LecturaException::class.java) { RankingParser.parsear(alterado) }
        }
    }
}
