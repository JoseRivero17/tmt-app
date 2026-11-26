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
    fun `con sesion lee las categorias, y la reserva sigue sin el envio real`() = runBlocking {
        val fuente = SitioInscripcionRemoteDataSource(ServicioFalso(html("inscripcionFormulario.html")))

        val opciones = fuente.opciones(11035) as ResultadoOpciones.Disponibles
        assertEquals(listOf("6ta", "8va", "4ta", "7ma"), opciones.opciones.map { it.categoria })
        // Falta ver el popup jugadores_inscripcionesForm.asp y su respuesta (docs/rf02-pendiente-cuenta.md).
        assertEquals(ResultadoReserva.ErrorSitio, fuente.reservar(11035, "4ta"))
    }

    private class ServicioFalso(private val respuesta: String) : InscripcionService {
        val pedidos = mutableListOf<Int>()

        override suspend fun formulario(torneoCodigo: Int): String {
            pedidos += torneoCodigo
            return respuesta
        }
    }
}
