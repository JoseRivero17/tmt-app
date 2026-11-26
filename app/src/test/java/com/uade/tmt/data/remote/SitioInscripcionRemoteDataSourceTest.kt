package com.uade.tmt.data.remote

import com.uade.tmt.data.remote.parser.html
import com.uade.tmt.domain.model.ResultadoOpciones
import com.uade.tmt.domain.model.ResultadoReserva
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

/** Sin red: el servicio devuelve HTML de muestra (RNF11). */
class SitioInscripcionRemoteDataSourceTest {

    private val servicio = ServicioFalso(html("inscripcion.html"))
    private val fuente = SitioInscripcionRemoteDataSource(servicio)

    @Test
    fun `sin sesion las opciones piden sesion`() = runBlocking {
        assertEquals(ResultadoOpciones.SesionRequerida, fuente.opciones(11036))
        assertEquals(listOf(11036), servicio.pedidos)
    }

    @Test
    fun `sin sesion la reserva pide sesion`() = runBlocking {
        assertEquals(ResultadoReserva.SesionRequerida, fuente.reservar(11036, "7ma"))
    }

    @Test
    fun `con sesion lee las categorias del sitio`() = runBlocking {
        val fuente = SitioInscripcionRemoteDataSource(ServicioFalso(html("inscripcionFormulario.html")))

        val opciones = fuente.opciones(11035) as ResultadoOpciones.Disponibles
        assertEquals(listOf("6ta", "8va", "4ta", "7ma"), opciones.opciones.map { it.categoria })
    }

    @Test
    fun `sin enviar al sitio, una categoria habilitada se da por reservada y no se manda nada`() = runBlocking {
        val servicio = ServicioFalso(html("inscripcionFormulario.html"))
        val fuente = SitioInscripcionRemoteDataSource(servicio)

        assertEquals(ResultadoReserva.Reservada, fuente.reservar(11035, "4ta"))
        assertEquals(emptyList<Map<String, String>>(), servicio.envios)
    }

    @Test
    fun `una categoria no habilitada se rechaza con el motivo del sitio`() = runBlocking {
        val fuente = SitioInscripcionRemoteDataSource(ServicioFalso(html("inscripcionFormulario.html")))

        assertEquals(
            ResultadoReserva.Rechazada("Tu rating no te habilita a inscribirte en esta competencia."),
            fuente.reservar(11035, "6ta"),
        )
    }

    @Test
    fun `enviando al sitio manda los campos del popup y, sin conocer la respuesta, no afirma nada`() = runBlocking {
        val servicio = ServicioFalso(html("inscripcionFormulario.html"))
        val fuente = SitioInscripcionRemoteDataSource(servicio, enviarAlSitio = true)

        assertEquals(ResultadoReserva.ErrorSitio, fuente.reservar(11035, "4ta"))
        assertEquals("19", servicio.envios.single()["categoria_cod"])
        assertEquals("on", servicio.envios.single()["chkDDJJ"])
    }

    private class ServicioFalso(private val respuesta: String) : InscripcionService {
        val pedidos = mutableListOf<Int>()
        val envios = mutableListOf<Map<String, String>>()

        override suspend fun formulario(torneoCodigo: Int): String {
            pedidos += torneoCodigo
            return respuesta
        }

        override suspend fun enviar(campos: Map<String, String>): String {
            envios += campos
            return ""
        }
    }
}
