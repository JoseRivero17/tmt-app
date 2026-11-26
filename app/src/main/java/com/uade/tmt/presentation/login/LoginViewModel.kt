package com.uade.tmt.presentation.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.uade.tmt.BuildConfig
import com.uade.tmt.TMTApplication
import com.uade.tmt.domain.model.ResultadoLogin
import com.uade.tmt.domain.repository.SesionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ErrorLogin { DATOS_INCOMPLETOS, CREDENCIALES_INVALIDAS, SIN_CONEXION, ERROR_SITIO }

/**
 * La clave vive solo en memoria, en este estado: no va al SavedStateHandle ni a `rememberSaveable`
 * (quedaría en el estado guardado del sistema) y [toString] no la incluye (RNF06).
 */
data class LoginUiState(
    val codigo: String = "",
    val clave: String = "",
    val cargando: Boolean = false,
    val error: ErrorLogin? = null,
    val exito: Boolean = false,
    /** Fuente de acceso falsa: la pantalla muestra el jugador de demostración. */
    val modoDemo: Boolean = false,
) {
    val puedeIngresar: Boolean get() = !cargando && !exito

    /** Los errores transitorios se pueden reintentar con los mismos datos. */
    val ofreceReintentar: Boolean
        get() = error == ErrorLogin.SIN_CONEXION || error == ErrorLogin.ERROR_SITIO

    override fun toString() =
        "LoginUiState(codigo=$codigo, clave=***, cargando=$cargando, error=$error, exito=$exito)"
}

/**
 * RF02. No recibe Context: todo lo que necesita llega por el AppContainer. [scope] solo se
 * cambia en los tests, que no tienen `Dispatchers.Main`.
 */
class LoginViewModel(
    private val sesiones: SesionRepository,
    modoDemo: Boolean,
    scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
) : ViewModel(scope) {

    private val _uiState = MutableStateFlow(LoginUiState(modoDemo = modoDemo))
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun cambiarCodigo(codigo: String) {
        _uiState.update { it.copy(codigo = codigo, error = null) }
    }

    fun cambiarClave(clave: String) {
        _uiState.update { it.copy(clave = clave, error = null) }
    }

    fun ingresar() {
        val estado = _uiState.value
        if (!estado.puedeIngresar) return
        _uiState.update { it.copy(cargando = true, error = null) }
        viewModelScope.launch {
            val resultado = sesiones.login(estado.codigo, estado.clave)
            _uiState.update {
                when (resultado) {
                    // La clave se borra apenas deja de hacer falta.
                    is ResultadoLogin.Ok -> it.copy(cargando = false, exito = true, clave = "")
                    ResultadoLogin.DatosIncompletos -> it.copy(cargando = false, error = ErrorLogin.DATOS_INCOMPLETOS)
                    ResultadoLogin.CredencialesInvalidas ->
                        it.copy(cargando = false, error = ErrorLogin.CREDENCIALES_INVALIDAS, clave = "")
                    ResultadoLogin.SinConexion -> it.copy(cargando = false, error = ErrorLogin.SIN_CONEXION)
                    ResultadoLogin.ErrorSitio -> it.copy(cargando = false, error = ErrorLogin.ERROR_SITIO)
                }
            }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as TMTApplication
                LoginViewModel(app.container.sesionRepository, modoDemo = !BuildConfig.SITIO_REAL)
            }
        }
    }
}
