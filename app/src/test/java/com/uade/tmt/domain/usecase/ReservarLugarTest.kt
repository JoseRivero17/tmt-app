package com.uade.tmt.domain.usecase

import com.uade.tmt.domain.model.Inscripcion
import com.uade.tmt.domain.model.OpcionInscripcion
import com.uade.tmt.domain.model.ResultadoLogin
import com.uade.tmt.domain.model.ResultadoOpciones
import com.uade.tmt.domain.model.ResultadoReserva
import com.uade.tmt.domain.model.SesionJugador
import com.uade.tmt.domain.repository.ReservaRepository
import com.uade.tmt.domain.repository.SesionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReservarLugarTest {

    private val sesiones = SesionesFalsas(SesionJugador("12345", "Jugador Demo"))
    private val reservas = ReservasFalsas(ResultadoReserva.Reservada)
    private val reservar = ReservarLugar(sesiones, reservas)

    private val habilitada = OpcionInscripcion("6ta", "12:00")

    @Test
    fun `sin sesion pide sesion sin consultar el sitio`() = runBlocking {
        sesiones.sesion.value = null

        assertEquals(ResultadoReserva.SesionRequerida, reservar(11036, habilitada, condicionesAceptadas = true))
        assertEquals(0, reservas.pedidos.size)
    }

    @Test
    fun `despues del login se retoma la misma reserva`() = runBlocking {
        sesiones.sesion.value = null
        assertEquals(ResultadoReserva.SesionRequerida, reservar(11036, habilitada, true))

        sesiones.sesion.value = SesionJugador("12345")

        assertEquals(ResultadoReserva.Reservada, reservar(11036, habilitada, true))
        assertEquals(listOf(11036 to "6ta"), reservas.pedidos)
    }

    @Test
    fun `sin aceptar las condiciones se rechaza sin consultar el sitio`() = runBlocking {
        assertEquals(
            ResultadoReserva.Rechazada(ReservarLugar.MOTIVO_CONDICIONES),
            reservar(11036, habilitada, condicionesAceptadas = false),
        )
        assertEquals(0, reservas.pedidos.size)
    }

    @Test
    fun `una categoria deshabilitada se rechaza con su motivo`() = runBlocking {
        val deshabilitada = OpcionInscripcion("5ta", habilitada = false, motivo = "Tu rating no habilita esta categoría")

        assertEquals(
            ResultadoReserva.Rechazada("Tu rating no habilita esta categoría"),
            reservar(11036, deshabilitada, true),
        )
        assertEquals(
            ResultadoReserva.Rechazada(ReservarLugar.MOTIVO_CATEGORIA),
            reservar(11036, deshabilitada.copy(motivo = null), true),
        )
        assertEquals(0, reservas.pedidos.size)
    }

    @Test
    fun `la sesion se valida antes que las condiciones y la categoria`() = runBlocking {
        sesiones.sesion.value = null
        val deshabilitada = habilitada.copy(habilitada = false)

        assertEquals(ResultadoReserva.SesionRequerida, reservar(11036, deshabilitada, condicionesAceptadas = false))
    }

    @Test
    fun `con todo valido devuelve lo que contesta el repositorio`() = runBlocking {
        for (resultado in listOf(ResultadoReserva.ListaEspera, ResultadoReserva.SinConexion, ResultadoReserva.ErrorSitio)) {
            reservas.respuesta = resultado
            assertEquals(resultado, reservar(11036, habilitada, true))
        }
    }

    @Test
    fun `si el sitio no reconoce la sesion se cierra la local`() = runBlocking {
        reservas.respuesta = ResultadoReserva.SesionRequerida

        assertEquals(ResultadoReserva.SesionRequerida, reservar(11036, habilitada, true))
        assertNull(sesiones.sesion.value)
    }

    private class SesionesFalsas(inicial: SesionJugador?) : SesionRepository {
        override val sesion = MutableStateFlow(inicial)

        override suspend fun login(codigo: String, clave: String): ResultadoLogin = error("No se usa")

        override suspend fun logout() {
            sesion.value = null
        }
    }

    private class ReservasFalsas(var respuesta: ResultadoReserva) : ReservaRepository {
        val pedidos = mutableListOf<Pair<Int, String>>()

        override suspend fun obtenerOpciones(torneoCodigo: Int): ResultadoOpciones = error("No se usa")

        override suspend fun reservar(torneoCodigo: Int, categoria: String): ResultadoReserva {
            pedidos += torneoCodigo to categoria
            return respuesta
        }

        override fun observarInscripciones(): Flow<List<Inscripcion>> = emptyFlow()
    }
}
