package com.uade.tmt.data.remote.parser

import com.uade.tmt.data.remote.LecturaException
import com.uade.tmt.domain.model.CategoriaTorneo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class CalendarioParserTest {

    private val html = html("calendario.html")

    @Test
    fun `lee todos los torneos con la fecha del dia al que pertenecen`() {
        val torneos = CalendarioParser.parsear(html)

        assertEquals(listOf(101, 102, 103), torneos.map { it.codigo })
        assertEquals(
            listOf(LocalDate.of(2026, 11, 27), LocalDate.of(2026, 11, 28), LocalDate.of(2026, 11, 28)),
            torneos.map { it.fecha },
        )
    }

    @Test
    fun `lee nombre, localidad y divisiones unificadas`() {
        val torneo = CalendarioParser.parsear(html).first()

        assertEquals("10° Torneo \"Copa de Muestra\"", torneo.nombre)
        assertEquals("Villa Ejemplo", torneo.localidad)
        assertEquals(
            listOf(CategoriaTorneo("7ma", hora = "20:30"), CategoriaTorneo("8va", hora = "21:00")),
            torneo.categorias,
        )
    }

    @Test
    fun `distingue los torneos sin inscripcion online`() {
        assertEquals(listOf(true, true, false), CalendarioParser.parsear(html).map { it.inscripcionOnline })
    }

    @Test
    fun `marca como cancelado solo el torneo que lo indica`() {
        val conCancelado = html.replace("Copa Sabatina", "Copa Sabatina - CANCELADO")

        val torneos = CalendarioParser.parsear(conCancelado)

        assertEquals(listOf(false, true, false), torneos.map { it.cancelado })
        assertFalse(torneos[1].admiteReserva)
        assertTrue(torneos[0].admiteReserva)
    }

    @Test
    fun `si cambia la estructura falla en lugar de devolver datos a medias`() {
        listOf(
            html.replace("linkNaranja", "otroNombre"),
            html.replace(Regex("""\d+ DE NOVIEMBRE DE \d+"""), "PROXIMAMENTE"),
            html.replace("Localidad:", "Ciudad:"),
            "<html><body>Sitio en mantenimiento</body></html>",
            "",
        ).forEach { alterado ->
            assertThrows(LecturaException::class.java) { CalendarioParser.parsear(alterado) }
        }
    }
}

/** HTML de muestra: misma estructura que el sitio, con nombres inventados. */
internal fun html(archivo: String): String =
    checkNotNull(object {}.javaClass.getResource("/html/$archivo")) { archivo }.readText()
