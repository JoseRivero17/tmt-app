package com.uade.tmt.data.remote.parser

import com.uade.tmt.data.remote.LecturaException
import com.uade.tmt.domain.model.OpcionInscripcion
import com.uade.tmt.domain.model.ResultadoOpciones
import com.uade.tmt.domain.model.ResultadoReserva
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class InscripcionParserTest {

    private val inscripcion = html("inscripcion.html")
    private val formulario = html("inscripcionFormulario.html")

    @Test
    fun `con sesion lista las categorias, su horario y por que no se pueden elegir`() {
        val motivo = "Tu rating no te habilita a inscribirte en esta competencia."
        assertEquals(
            ResultadoOpciones.Disponibles(
                listOf(
                    OpcionInscripcion("6ta", "13:30", habilitada = false, motivo = motivo),
                    OpcionInscripcion("8va", "14:00", habilitada = false, motivo = motivo),
                    OpcionInscripcion("4ta", "16:00", habilitada = true),
                    OpcionInscripcion("7ma", "16:30", habilitada = false, motivo = motivo),
                ),
            ),
            InscripcionParser.opciones(formulario),
        )
    }

    @Test
    fun `el formulario con sesion no pide sesion`() {
        assertFalse(InscripcionParser.pideSesion(formulario))
    }

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
    fun `una pagina sin categorias no se adivina y la respuesta al envio sigue sin conocerse`() {
        assertThrows(LecturaException::class.java) { InscripcionParser.opciones(inscripcion) }
        assertEquals(ResultadoReserva.ErrorSitio, InscripcionParser.resultadoReserva(inscripcion))
    }
}
