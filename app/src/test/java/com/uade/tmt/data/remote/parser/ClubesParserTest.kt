package com.uade.tmt.data.remote.parser

import com.uade.tmt.data.remote.LecturaException
import com.uade.tmt.domain.model.Club
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ClubesParserTest {

    private val html = html("clubes_mapaTodos.html")

    @Test
    fun `lee todos los clubes del mapa`() {
        assertEquals(210, ClubesParser.parsear(html).size)
    }

    @Test
    fun `cada club trae codigo, nombre, localidad y coordenadas`() {
        val club = ClubesParser.parsear(html).first { it.codigo == 81 }

        assertEquals(
            Club(81, "Adrogue Sith Spin", localidad = "Adrogue", latitud = -34.796361, longitud = -58.386436),
            club,
        )
    }

    @Test
    fun `descarta las asociaciones aunque repitan el codigo de un club`() {
        val clubes = ClubesParser.parsear(html)

        assertTrue(clubes.none { it.nombre.startsWith("ZN (") || it.nombre.contains("Paranaense") })
        assertEquals("Ntra. Sra. de Lourdes", clubes.single { it.codigo == 7 }.nombre)
    }

    @Test
    fun `todos los clubes de la muestra tienen ubicacion`() {
        assertTrue(ClubesParser.parsear(html).all { it.latitud != null && it.longitud != null })
    }

    @Test
    fun `ignora el zoom que algunos clubes agregan a las coordenadas`() {
        val club = ClubesParser.parsear(html).first { it.codigo == 100 }

        assertEquals(-34.7766715, club.latitud!!, 0.0)
        assertEquals(-58.631449, club.longitud!!, 0.0)
    }

    @Test
    fun `un club sin coordenadas validas se lee igual, sin ubicacion`() {
        val sinMapa = html.replace("'-34.796361, -58.386436'", "', '")
        val club = ClubesParser.parsear(sinMapa).first { it.codigo == 81 }

        assertNull(club.latitud)
        assertNull(club.longitud)
    }

    @Test
    fun `coordenadas en cero o fuera de rango se descartan`() {
        assertNull(ClubesParser.coordenadas("'0, 0'"))
        assertNull(ClubesParser.coordenadas("'-134.5, -58.3'"))
        assertEquals(-34.5 to -58.25, ClubesParser.coordenadas("goto('#mapa', map,'-34.5, -58.25')"))
    }

    @Test
    fun `si cambia la estructura falla`() {
        listOf(
            html.replace("clubes_ampliar.asp", "club_ficha.asp"),
            html.replace("linkNaranja", "linkRojo"),
            "<html><body>Error</body></html>",
        ).forEach { alterado ->
            assertThrows(LecturaException::class.java) { ClubesParser.parsear(alterado) }
        }
    }
}
