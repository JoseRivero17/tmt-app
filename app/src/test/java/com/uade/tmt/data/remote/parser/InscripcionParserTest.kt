package com.uade.tmt.data.remote.parser

import com.uade.tmt.domain.model.ResultadoOpciones
import com.uade.tmt.domain.model.ResultadoReserva
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InscripcionParserTest {

    private val inscripcion = html("inscripcion.html")

    @Test
    fun `inscripciones sin sesion pide sesion`() {
        assertTrue(InscripcionParser.pideSesion(inscripcion))
    }

    @Test
    fun `una pagina publica con el formulario de acceso en el encabezado pide sesion`() {
        assertTrue(InscripcionParser.pideSesion(html("TenisMesaWeb.html")))
    }

    @Test
    fun `cualquier mensaje de la pantalla de acceso pide sesion`() {
        val vencida = inscripcion.replace(
            "Debes ingresar como usuario para acceder a la seccion de inscripciones online",
            "Tu sesion expiro",
        )

        assertTrue(InscripcionParser.pideSesion(vencida))
    }

    @Test
    fun `una pagina que no se entiende no se toma como falta de sesion`() {
        assertFalse(InscripcionParser.pideSesion("<html><body>Mantenimiento</body></html>"))
        assertFalse(InscripcionParser.pideSesion(""))
    }

    @Test
    fun `sin la pagina real del formulario las opciones son error del sitio`() {
        assertEquals(ResultadoOpciones.ErrorSitio, InscripcionParser.opciones(inscripcion))
        assertEquals(ResultadoReserva.ErrorSitio, InscripcionParser.resultadoReserva(inscripcion))
    }
}
