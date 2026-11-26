package com.uade.tmt.presentation.clubes.detalle

import com.uade.tmt.domain.model.Club
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EnlacesClubTest {

    private val club = Club(
        codigo = 81,
        nombre = "Adrogue Sith Spin",
        direccion = "Bynnon 1649",
        localidad = "Adrogue",
        telefono = "11 2159-1685",
        latitud = -34.796361,
        longitud = -58.386436,
    )

    @Test
    fun `como llegar usa las coordenadas con el nombre del club`() {
        assertEquals(
            "geo:-34.796361,-58.386436?q=-34.796361,-58.386436(Adrogue+Sith+Spin)",
            uriComoLlegar(club),
        )
    }

    @Test
    fun `sin coordenadas busca por direccion y localidad`() {
        val sinMapa = club.copy(latitud = null, longitud = null)

        assertEquals("geo:0,0?q=Bynnon+1649%2C+Adrogue", uriComoLlegar(sinMapa))
        assertEquals("https://www.google.com/maps/search/?api=1&query=Bynnon+1649%2C+Adrogue", urlMapaWeb(sinMapa))
    }

    @Test
    fun `sin coordenadas ni direccion no hay como llegar`() {
        val sinDatos = club.copy(latitud = null, longitud = null, direccion = null)

        assertNull(uriComoLlegar(sinDatos))
        assertNull(urlMapaWeb(sinDatos))
    }

    @Test
    fun `llamar deja solo los digitos del telefono`() {
        assertEquals("tel:1121591685", uriLlamar(club))
        assertNull(uriLlamar(club.copy(telefono = null)))
        assertNull(uriLlamar(club.copy(telefono = "-")))
    }
}
