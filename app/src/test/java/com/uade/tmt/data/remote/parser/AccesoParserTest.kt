package com.uade.tmt.data.remote.parser

import com.uade.tmt.data.remote.EstadoAcceso
import com.uade.tmt.data.remote.LecturaException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class AccesoParserTest {

    private val home = html("TenisMesaWeb.html")
    private val datosIncompletos = html("buscarJugador.html")
    private val inscripcion = html("inscripcion.html")

    @Test
    fun `la home publica es una pagina sin sesion`() {
        assertEquals(EstadoAcceso.SinSesion, AccesoParser.parsear(home))
    }

    @Test
    fun `el aviso de datos incompletos se reconoce`() {
        assertEquals(EstadoAcceso.DatosIncompletos, AccesoParser.parsear(datosIncompletos))
    }

    @Test
    fun `inscripciones sin sesion pide ingresar como usuario`() {
        assertEquals(EstadoAcceso.SesionRequerida, AccesoParser.parsear(inscripcion))
    }

    @Test
    fun `la pantalla de acceso con otro mensaje son credenciales invalidas`() {
        val otro = datosIncompletos.replace("Debes completar todos tus datos de acceso", "Clave incorrecta")

        assertEquals(EstadoAcceso.CredencialesInvalidas, AccesoParser.parsear(otro))
    }

    @Test
    fun `sin titulo de acceso, sin mensaje y sin campo de clave es exito`() {
        val conSesion = home.replace(CAMPO_CLAVE_HOME, "")

        assertEquals(EstadoAcceso.Exito(nombre = null), AccesoParser.parsear(conSesion))
    }

    @Test
    fun `pantalla de acceso sin su formulario es ilegible`() {
        val alterado = datosIncompletos.replace(FORMULARIO_ACCESO, "")

        assertThrows(LecturaException::class.java) { AccesoParser.parsear(alterado) }
    }

    @Test
    fun `una pagina sin la barra superior del sitio es ilegible y no exito`() {
        val mantenimiento = "<html><body><p>Sitio en mantenimiento</p></body></html>"

        assertThrows(LecturaException::class.java) { AccesoParser.parsear(mantenimiento) }
    }

    @Test
    fun `una respuesta vacia es ilegible`() {
        assertThrows(LecturaException::class.java) { AccesoParser.parsear("") }
    }

    private companion object {
        val CAMPO_CLAVE_HOME = """<input name="clave" type="password" id="clave25" size="4" class="campo">"""

        /** El segundo formulario de la pantalla de acceso (el del encabezado lleva `&accesoURL=`). */
        val FORMULARIO_ACCESO = Regex(
            """<form method="post" action="jugadores_acceso_paso2\.asp\?accion=ingresar">.*?</form>""",
            RegexOption.DOT_MATCHES_ALL,
        )
    }
}
