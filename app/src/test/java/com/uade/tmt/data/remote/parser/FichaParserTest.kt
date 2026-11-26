package com.uade.tmt.data.remote.parser

import com.uade.tmt.data.remote.LecturaException
import com.uade.tmt.domain.model.Jugador
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class FichaParserTest {

    private val html = html("ficha.html")

    @Test
    fun `lee nombre, rating, division, variacion y club`() {
        assertEquals(
            Jugador(
                codigo = 1001,
                nombre = "Ana Muñoz",
                division = "6ta",
                puesto = null,
                rating = 1500,
                variacion = 12,
                clubCodigo = 90,
                club = "CLU",
            ),
            FichaParser.parsear(1001, html),
        )
    }

    @Test
    fun `la posicion de la ficha es general, asi que no se usa como puesto en la division`() {
        assertNull(FichaParser.parsear(1001, html).puesto)
    }

    @Test
    fun `no confunde el codigo de la ficha con el campo de codigo del login`() {
        assertEquals(1001, FichaParser.parsear(1001, html).codigo)
    }

    @Test
    fun `si llega la ficha de otro jugador no la guarda como la pedida`() {
        assertThrows(LecturaException::class.java) { FichaParser.parsear(2002, html) }
    }

    @Test
    fun `variacion con guion queda sin dato`() {
        val sinVariacion = html.replace("<strong>+12</a></strong>", "<strong>-</a></strong>")
        assertNull(FichaParser.parsear(1001, sinVariacion).variacion)
    }

    @Test
    fun `un jugador sin club se lee igual`() {
        val sinClub = html.replace(Regex("""<a href="clubes_ampliar[^>]*>.*?</a>\s*<img[^>]*>"""), "")
        val jugador = FichaParser.parsear(1001, sinClub)
        assertNull(jugador.club)
        assertNull(jugador.clubCodigo)
    }

    @Test
    fun `una pagina que no es una ficha no se adivina`() {
        assertThrows(LecturaException::class.java) { FichaParser.parsear(1001, html("calendario.html")) }
        assertThrows(LecturaException::class.java) { FichaParser.parsear(1001, "") }
    }
}
