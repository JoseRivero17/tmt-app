package com.uade.tmt.data.remote.parser

import com.uade.tmt.data.remote.LecturaException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class ClubParserTest {

    private val html = html("clubes_ampliar.html")

    @Test
    fun `lee la ficha del club`() {
        val club = ClubParser.parsear(81, html)

        assertEquals(81, club.codigo)
        assertEquals("Adrogue Sith Spin", club.nombre)
        assertEquals("Bynnon 1649 (entre Drumond y plaza bynon)", club.direccion)
        assertEquals("Adrogue", club.localidad)
        assertEquals("1100000000", club.telefono)
        assertEquals(-34.796361, club.latitud!!, 0.0)
        assertEquals(-58.386436, club.longitud!!, 0.0)
    }

    @Test
    fun `el telefono y el mapa son opcionales`() {
        val sinDatos = html
            .replace("1100000000 |", "|")
            .replace("var lat = -34.796361;", "var lat = ;")

        val club = ClubParser.parsear(81, sinDatos)

        assertNull(club.telefono)
        assertNull(club.latitud)
        assertEquals("Bynnon 1649 (entre Drumond y plaza bynon)", club.direccion)
    }

    @Test
    fun `si cambia la estructura falla`() {
        listOf(
            html.replace("Direcci&oacute;n:", "Domicilio:"),
            html.replace("border-right", "border-left"),
            "<html><body>Error</body></html>",
        ).forEach { alterado ->
            assertThrows(LecturaException::class.java) { ClubParser.parsear(81, alterado) }
        }
    }
}
