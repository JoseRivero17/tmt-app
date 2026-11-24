package com.uade.tmt.data.remote.parser

import com.uade.tmt.data.remote.LecturaException
import com.uade.tmt.domain.model.CategoriaTorneo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class TorneoParserTest {

    private val html = html("torneo.html")

    @Test
    fun `lee los datos del torneo`() {
        val torneo = TorneoParser.parsear(101, html)

        assertEquals("10° Torneo \"Copa de Muestra\"", torneo.nombre)
        assertEquals(LocalDate.of(2026, 11, 27), torneo.fecha)
        assertEquals(77, torneo.clubCodigo)
        assertEquals("Club de Muestra", torneo.sede)
        assertEquals("Calle Inventada 123 - VILLA EJEMPLO Capital Federal", torneo.direccion)
        assertEquals("$1000.- inscripcion general\n$800.- para jugadores del club", torneo.precios)
        assertTrue(torneo.inscripcionOnline)
        assertFalse(torneo.cancelado)
    }

    @Test
    fun `lee divisiones con formato, horario y cupos`() {
        assertEquals(
            listOf(
                CategoriaTorneo("7ma", "3 sets, final 5 sets", "20:30", 18),
                CategoriaTorneo("8va", "3 sets, final 5 sets", "21:00", 12),
            ),
            TorneoParser.parsear(101, html).categorias,
        )
    }

    @Test
    fun `una mencion a cancelaciones en el texto libre no cancela el torneo`() {
        val torneo = TorneoParser.parsear(101, html.replace("8 mesas", "Si se cancela, se avisa. Torneo cancelado en 2020"))

        assertFalse(torneo.cancelado)
    }

    @Test
    fun `el aviso de cancelacion en el encabezado cancela el torneo`() {
        assertTrue(TorneoParser.parsear(101, html.replace("Copa de Muestra", "Copa de Muestra (SUSPENDIDO)")).cancelado)
    }

    @Test
    fun `un cupo negativo publicado por el sitio se trata como dato faltante`() {
        val torneo = TorneoParser.parsear(101, html.replace("3 mesas (Cupos: 18)", "-4 mesas (Cupos: -24)"))

        assertNull(torneo.categorias.first().cupos)
        assertEquals("20:30", torneo.categorias.first().hora)
    }

    @Test
    fun `los datos opcionales pueden faltar`() {
        val torneo = TorneoParser.parsear(101, html.replace("Sede:", "Lugar:").replace("Inscripción:", "Valor:"))

        assertNull(torneo.sede)
        assertNull(torneo.precios)
    }

    @Test
    fun `si cambia la estructura falla`() {
        listOf(
            html.replace("Divisiones:", "Categorias:"),
            html.replace("27 de Noviembre de 2026", "a confirmar"),
            "<html><body>Error</body></html>",
        ).forEach { alterado ->
            assertThrows(LecturaException::class.java) { TorneoParser.parsear(101, alterado) }
        }
    }
}
