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
    fun `con sesion, mientras falte el formulario real, es error del sitio`() = runBlocking {
        val conSesion = html("TenisMesaWeb.html")
            .replace("""<input name="clave" type="password" id="clave25" size="4" class="campo">""", "")
        val fuente = SitioInscripcionRemoteDataSource(ServicioFalso(conSesion))

        assertEquals(ResultadoOpciones.ErrorSitio, fuente.opciones(11036))
        assertEquals(ResultadoReserva.ErrorSitio, fuente.reservar(11036, "7ma"))
    }

    private class ServicioFalso(private val respuesta: String) : InscripcionService {
        val pedidos = mutableListOf<Int>()

        override suspend fun formulario(torneoCodigo: Int): String {
            pedidos += torneoCodigo
            return respuesta
        }
    }
}
