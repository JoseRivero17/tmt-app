package com.uade.tmt.presentation.login

import com.uade.tmt.domain.model.ResultadoLogin
import com.uade.tmt.domain.model.SesionJugador
import com.uade.tmt.domain.repository.SesionRepository
import com.uade.tmt.domain.usecase.IniciarSesion
import com.uade.tmt.domain.usecase.JugadoresFalsos
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Sin Android ni `Dispatchers.Main`: el ViewModel corre en un scope Unconfined (RNF11). */
class LoginViewModelTest {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
    private val sesiones = SesionesFalsas()
    private val jugadores = JugadoresFalsos()

    @After
    fun cerrar() = scope.cancel()

    private fun viewModel(modoDemo: Boolean = true) =
        LoginViewModel(IniciarSesion(sesiones, jugadores), modoDemo, scope)

    @Test
    fun `login exitoso marca exito, borra la clave y deja la sesion`() {
        sesiones.respuesta = { codigo -> ResultadoLogin.Ok(SesionJugador(codigo, "Jugador Demo")) }
        val vm = viewModel()

        vm.cambiarCodigo("12345")
        vm.cambiarClave("demo")
        vm.ingresar()

        val estado = vm.uiState.value
        assertTrue(estado.exito)
        assertFalse(estado.cargando)
        assertNull(estado.error)
        assertEquals("", estado.clave)
        assertEquals(listOf("12345" to "demo"), sesiones.intentos)
        assertEquals(SesionJugador("12345", "Jugador Demo"), sesiones.sesion.value)
        // RF03: quien inicia sesión queda como perfil propio.
        assertEquals(12345, jugadores.perfilPropio)
    }

    @Test
    fun `credenciales invalidas muestran el error, borran la clave y no ofrecen reintentar`() {
        sesiones.respuesta = { ResultadoLogin.CredencialesInvalidas }
        val vm = viewModel()

        vm.cambiarCodigo("12345")
        vm.cambiarClave("otra")
        vm.ingresar()

        val estado = vm.uiState.value
        assertEquals(ErrorLogin.CREDENCIALES_INVALIDAS, estado.error)
        assertFalse(estado.exito)
        assertFalse(estado.ofreceReintentar)
        assertEquals("", estado.clave)
        assertEquals("12345", estado.codigo)
    }

    @Test
    fun `cada resultado de login tiene su error`() {
        val casos = mapOf(
            ResultadoLogin.DatosIncompletos to ErrorLogin.DATOS_INCOMPLETOS,
            ResultadoLogin.SinConexion to ErrorLogin.SIN_CONEXION,
            ResultadoLogin.ErrorSitio to ErrorLogin.ERROR_SITIO,
        )
        for ((resultado, error) in casos) {
            sesiones.respuesta = { resultado }
            val vm = viewModel()
            vm.ingresar()
            assertEquals(error, vm.uiState.value.error)
        }
    }

    @Test
    fun `error del sitio ofrece reintentar con los mismos datos`() {
        sesiones.respuesta = { ResultadoLogin.ErrorSitio }
        val vm = viewModel()
        vm.cambiarCodigo("12345")
        vm.cambiarClave("demo")
        vm.ingresar()
        assertTrue(vm.uiState.value.ofreceReintentar)

        sesiones.respuesta = { ResultadoLogin.Ok(SesionJugador(it)) }
        vm.ingresar()

        assertTrue(vm.uiState.value.exito)
        assertEquals(listOf("12345" to "demo", "12345" to "demo"), sesiones.intentos)
    }

    @Test
    fun `editar un campo borra el error`() {
        sesiones.respuesta = { ResultadoLogin.CredencialesInvalidas }
        val vm = viewModel()
        vm.ingresar()

        vm.cambiarClave("x")

        assertNull(vm.uiState.value.error)
    }

    @Test
    fun `el estado nunca expone la clave en su texto`() {
        val vm = viewModel()
        vm.cambiarClave("secreta")

        assertFalse(vm.uiState.value.toString().contains("secreta"))
    }

    @Test
    fun `el modo demo llega al estado`() {
        assertTrue(viewModel(modoDemo = true).uiState.value.modoDemo)
        assertFalse(viewModel(modoDemo = false).uiState.value.modoDemo)
    }

    private class SesionesFalsas : SesionRepository {
        override val sesion = MutableStateFlow<SesionJugador?>(null)
        var respuesta: (String) -> ResultadoLogin = { ResultadoLogin.ErrorSitio }
        val intentos = mutableListOf<Pair<String, String>>()

        override suspend fun login(codigo: String, clave: String): ResultadoLogin {
            intentos += codigo to clave
            return respuesta(codigo).also { if (it is ResultadoLogin.Ok) sesion.value = it.sesion }
        }

        override suspend fun logout() {
            sesion.value = null
        }
    }
}
