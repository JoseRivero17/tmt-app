package com.uade.tmt.presentation.reserva

import androidx.lifecycle.SavedStateHandle
import com.uade.tmt.domain.model.Inscripcion
import com.uade.tmt.domain.model.OpcionInscripcion
import com.uade.tmt.domain.model.ResultadoLogin
import com.uade.tmt.domain.model.ResultadoOpciones
import com.uade.tmt.domain.model.ResultadoReserva
import com.uade.tmt.domain.model.SesionJugador
import com.uade.tmt.domain.repository.Conectividad
import com.uade.tmt.domain.repository.ReservaRepository
import com.uade.tmt.domain.repository.SesionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Sin Android ni `Dispatchers.Main`: el ViewModel corre en un scope Unconfined (RNF11). */
class ReservaViewModelTest {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
    private val sesiones = SesionesFalsas(SesionJugador("12345", "Jugador Demo"))
    private val reservas = ReservasFalsas()
    private val conectividad = ConectividadFalsa()
    private val guardado = SavedStateHandle()

    @After
    fun cerrar() = scope.cancel()

    private fun viewModel(handle: SavedStateHandle = guardado) =
        ReservaViewModel(TORNEO, reservas, sesiones, conectividad, handle, scope = scope)

    @Test
    fun `al abrir carga las opciones`() {
        val estado = viewModel().uiState.value

        assertEquals(CargaOpciones.LISTA, estado.carga)
        assertEquals(OPCIONES, estado.opciones)
        assertNull(estado.seleccionada)
        assertFalse(estado.puedeConfirmar)
    }

    @Test
    fun `reserva exitosa en tres interacciones`() {
        val vm = viewModel()

        vm.elegir("6ta")
        vm.aceptarCondiciones(true)
        assertTrue(vm.uiState.value.puedeConfirmar)
        vm.confirmar()

        val estado = vm.uiState.value
        assertEquals(ResultadoReserva.Reservada, estado.resultado)
        assertTrue(estado.terminada)
        assertEquals("12:00", estado.opcionElegida?.horario)
        assertEquals(listOf(TORNEO to "6ta"), reservas.pedidos)
    }

    @Test
    fun `con una sola categoria habilitada ya viene elegida`() {
        reservas.opciones = ResultadoOpciones.Disponibles(
            listOf(OPCIONES[0], OpcionInscripcion("6ta", "12:00")),
        )
        val vm = viewModel()

        assertEquals("6ta", vm.uiState.value.seleccionada)
        vm.aceptarCondiciones(true)
        vm.confirmar()
        assertEquals(ResultadoReserva.Reservada, vm.uiState.value.resultado)
    }

    @Test
    fun `una categoria deshabilitada no se puede elegir`() {
        val vm = viewModel()

        vm.elegir("5ta")

        assertNull(vm.uiState.value.seleccionada)
    }

    @Test
    fun `sin aceptar las condiciones o sin conexion no se puede confirmar`() {
        val vm = viewModel()
        vm.elegir("6ta")
        assertFalse(vm.uiState.value.puedeConfirmar)

        vm.aceptarCondiciones(true)
        conectividad.conectado.value = false
        assertFalse(vm.uiState.value.puedeConfirmar)
        vm.confirmar()

        assertTrue(reservas.pedidos.isEmpty())
    }

    @Test
    fun `sesion requerida va al login y despues retoma la misma reserva`() {
        sesiones.sesion.value = null
        val vm = viewModel()
        vm.elegir("6ta")
        vm.aceptarCondiciones(true)

        vm.confirmar()

        assertTrue(vm.uiState.value.irALogin)
        assertEquals(Pendiente.RESERVA, vm.uiState.value.pendiente)
        assertTrue(reservas.pedidos.isEmpty())

        vm.loginAbierto()
        sesiones.sesion.value = SesionJugador("12345") // login exitoso en LoginScreen

        val estado = vm.uiState.value
        assertEquals(ResultadoReserva.Reservada, estado.resultado)
        assertNull(estado.pendiente)
        assertEquals(listOf(TORNEO to "6ta"), reservas.pedidos)
        assertEquals(2, reservas.cargas) // se recargan las opciones con la sesión nueva
    }

    @Test
    fun `si vuelve del login sin ingresar puede volver a pedirlo`() {
        sesiones.sesion.value = null
        val vm = viewModel()
        vm.elegir("6ta")
        vm.aceptarCondiciones(true)
        vm.confirmar()
        vm.loginAbierto()

        assertTrue(vm.uiState.value.pideLogin)
        vm.iniciarSesion()
        assertTrue(vm.uiState.value.irALogin)
    }

    @Test
    fun `la reserva pendiente sobrevive a que se recree el ViewModel`() {
        sesiones.sesion.value = null
        val primero = viewModel()
        primero.elegir("6ta")
        primero.aceptarCondiciones(true)
        primero.confirmar()

        // Mismo SavedStateHandle: la entrada de la ruta Reserva se restauró.
        val segundo = viewModel()
        assertEquals("6ta", segundo.uiState.value.seleccionada)
        assertTrue(segundo.uiState.value.condicionesAceptadas)
        assertEquals(Pendiente.RESERVA, segundo.uiState.value.pendiente)

        sesiones.sesion.value = SesionJugador("12345")
        assertEquals(ResultadoReserva.Reservada, segundo.uiState.value.resultado)
    }

    @Test
    fun `opciones que piden sesion van al login y se recargan al volver`() {
        sesiones.sesion.value = null
        reservas.opciones = ResultadoOpciones.SesionRequerida
        val vm = viewModel()

        assertEquals(CargaOpciones.SESION_REQUERIDA, vm.uiState.value.carga)
        assertTrue(vm.uiState.value.irALogin)
        vm.loginAbierto()

        reservas.opciones = ResultadoOpciones.Disponibles(OPCIONES)
        sesiones.sesion.value = SesionJugador("12345")

        assertEquals(CargaOpciones.LISTA, vm.uiState.value.carga)
        assertNull(vm.uiState.value.pendiente)
        assertTrue(reservas.pedidos.isEmpty())
    }

    @Test
    fun `incierta no ofrece reintentar ni permite confirmar de nuevo`() {
        reservas.respuesta = ResultadoReserva.Incierta
        val vm = viewModel()
        vm.elegir("6ta")
        vm.aceptarCondiciones(true)
        vm.confirmar()

        val estado = vm.uiState.value
        assertEquals(ResultadoReserva.Incierta, estado.resultado)
        assertFalse(estado.ofreceReintentar)
        assertFalse(estado.puedeConfirmar)

        vm.reintentar()
        vm.confirmar()
        assertEquals(1, reservas.pedidos.size)
    }

    @Test
    fun `sin conexion o error del sitio ofrecen reintentar`() {
        for (resultado in listOf(ResultadoReserva.SinConexion, ResultadoReserva.ErrorSitio)) {
            reservas.respuesta = resultado
            reservas.pedidos.clear()
            val vm = viewModel(SavedStateHandle())
            vm.elegir("6ta")
            vm.aceptarCondiciones(true)
            vm.confirmar()
            assertTrue(vm.uiState.value.ofreceReintentar)

            reservas.respuesta = ResultadoReserva.Reservada
            vm.reintentar()

            assertEquals(ResultadoReserva.Reservada, vm.uiState.value.resultado)
            assertEquals(2, reservas.pedidos.size)
        }
    }

    @Test
    fun `rechazada muestra el motivo y deja elegir otra categoria`() {
        reservas.respuesta = ResultadoReserva.Rechazada("Sin cupo")
        val vm = viewModel()
        vm.elegir("6ta")
        vm.aceptarCondiciones(true)
        vm.confirmar()

        assertEquals(ResultadoReserva.Rechazada("Sin cupo"), vm.uiState.value.resultado)
        assertFalse(vm.uiState.value.terminada)

        vm.elegir("7ma")
        assertNull(vm.uiState.value.resultado)
        assertTrue(vm.uiState.value.puedeConfirmar)
    }

    @Test
    fun `sin conexion al cargar las opciones se puede recargar`() {
        reservas.opciones = ResultadoOpciones.SinConexion
        val vm = viewModel()
        assertEquals(CargaOpciones.SIN_CONEXION, vm.uiState.value.carga)

        reservas.opciones = ResultadoOpciones.Disponibles(OPCIONES)
        vm.cargarOpciones()

        assertEquals(CargaOpciones.LISTA, vm.uiState.value.carga)
    }

    private companion object {
        const val TORNEO = 11036
        val OPCIONES = listOf(
            OpcionInscripcion("5ta", "10:00", habilitada = false, motivo = "Tu rating no habilita esta categoría"),
            OpcionInscripcion("6ta", "12:00"),
            OpcionInscripcion("7ma", "14:00"),
        )
    }

    private class SesionesFalsas(inicial: SesionJugador?) : SesionRepository {
        override val sesion = MutableStateFlow(inicial)

        override suspend fun login(codigo: String, clave: String): ResultadoLogin = error("No se usa")

        override suspend fun logout() {
            sesion.value = null
        }
    }

    private class ReservasFalsas : ReservaRepository {
        var opciones: ResultadoOpciones = ResultadoOpciones.Disponibles(OPCIONES)
        var respuesta: ResultadoReserva = ResultadoReserva.Reservada
        var cargas = 0
        val pedidos = mutableListOf<Pair<Int, String>>()

        override suspend fun obtenerOpciones(torneoCodigo: Int): ResultadoOpciones {
            cargas++
            return opciones
        }

        override suspend fun reservar(torneoCodigo: Int, categoria: String): ResultadoReserva {
            pedidos += torneoCodigo to categoria
            return respuesta
        }

        override fun observarInscripciones(): Flow<List<Inscripcion>> = emptyFlow()
    }

    private class ConectividadFalsa : Conectividad {
        override val conectado = MutableStateFlow(true)
    }
}
