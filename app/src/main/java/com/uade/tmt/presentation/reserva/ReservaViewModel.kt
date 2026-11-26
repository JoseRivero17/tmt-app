package com.uade.tmt.presentation.reserva

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.toRoute
import com.uade.tmt.TMTApplication
import com.uade.tmt.domain.model.ResultadoOpciones
import com.uade.tmt.domain.model.ResultadoReserva
import com.uade.tmt.domain.repository.Conectividad
import com.uade.tmt.domain.repository.ReservaRepository
import com.uade.tmt.domain.repository.SesionRepository
import com.uade.tmt.domain.usecase.ReservarLugar
import com.uade.tmt.presentation.navigation.Reserva
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * RF02. Vive con la entrada de la ruta [Reserva] en la pila: sigue vivo mientras el jugador está
 * en el login, y al aparecer la sesión retoma lo que quedó [ReservaUiState.pendiente] (criterio 7).
 * La categoría elegida, las condiciones y lo pendiente se copian al [SavedStateHandle] para
 * sobrevivir también a que el sistema mate el proceso.
 *
 * No recibe Context. [scope] solo se cambia en los tests, que no tienen `Dispatchers.Main`.
 */
class ReservaViewModel(
    private val torneoCodigo: Int,
    private val reservas: ReservaRepository,
    private val sesiones: SesionRepository,
    conectividad: Conectividad,
    private val guardado: SavedStateHandle,
    private val reservarLugar: ReservarLugar = ReservarLugar(sesiones, reservas),
    envioSimulado: Boolean = false,
    scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
) : ViewModel(scope) {

    private val _uiState = MutableStateFlow(
        ReservaUiState(
            torneoCodigo = torneoCodigo,
            seleccionada = guardado[CLAVE_SELECCIONADA],
            condicionesAceptadas = guardado[CLAVE_CONDICIONES] ?: false,
            pendiente = guardado.get<String>(CLAVE_PENDIENTE)?.let(Pendiente::valueOf),
            envioSimulado = envioSimulado,
        ),
    )
    val uiState: StateFlow<ReservaUiState> = _uiState.asStateFlow()

    private var carga: Job? = null

    init {
        viewModelScope.launch {
            conectividad.conectado.collect { conectado -> _uiState.update { it.copy(conectado = conectado) } }
        }
        // Solo los cambios: el valor actual lo considera la primera carga.
        viewModelScope.launch {
            sesiones.sesion.drop(1).collect { sesion -> if (sesion != null) retomar() }
        }
        val estado = _uiState.value
        cargar(reanudarReserva = estado.pendiente == Pendiente.RESERVA && sesiones.sesion.value != null)
    }

    fun cargarOpciones() = cargar(reanudarReserva = false)

    fun elegir(categoria: String) {
        val estado = _uiState.value
        if (estado.enviando || estado.terminada) return
        if (estado.opciones.none { it.categoria == categoria && it.habilitada }) return
        _uiState.update { it.copy(seleccionada = categoria, resultado = null) }
        guardado[CLAVE_SELECCIONADA] = categoria
    }

    fun aceptarCondiciones(aceptadas: Boolean) {
        if (_uiState.value.terminada) return
        _uiState.update { it.copy(condicionesAceptadas = aceptadas) }
        guardado[CLAVE_CONDICIONES] = aceptadas
    }

    fun confirmar() {
        val estado = _uiState.value
        val opcion = estado.opcionElegida
        if (!estado.puedeConfirmar || opcion == null) return
        _uiState.update { it.copy(enviando = true, resultado = null) }
        viewModelScope.launch {
            val resultado = reservarLugar(torneoCodigo, opcion, estado.condicionesAceptadas)
            if (resultado == ResultadoReserva.SesionRequerida) {
                _uiState.update { it.copy(enviando = false) }
                pedirLogin(Pendiente.RESERVA)
            } else {
                _uiState.update { it.copy(enviando = false, resultado = resultado, pendiente = null) }
                guardado[CLAVE_PENDIENTE] = null
            }
        }
    }

    /** Solo para [ReservaUiState.ofreceReintentar]: nunca después de una reserva incierta. */
    fun reintentar() {
        if (_uiState.value.ofreceReintentar) confirmar()
    }

    /** El jugador volvió del login sin ingresar y pide ir de nuevo. */
    fun iniciarSesion() {
        _uiState.update { it.copy(irALogin = true) }
    }

    /** La UI ya navegó al login: el evento no se repite al recomponer. */
    fun loginAbierto() {
        _uiState.update { it.copy(irALogin = false) }
    }

    private fun pedirLogin(pendiente: Pendiente) {
        _uiState.update { it.copy(pendiente = pendiente, irALogin = true) }
        guardado[CLAVE_PENDIENTE] = pendiente.name
    }

    /**
     * Hay sesión nueva. Siempre se vuelven a pedir las opciones (con sesión pueden cambiar las
     * habilitadas) y, si lo pendiente era la reserva, se envía la misma categoría si sigue habilitada.
     */
    private fun retomar() {
        val pendiente = _uiState.value.pendiente ?: return
        cargar(reanudarReserva = pendiente == Pendiente.RESERVA)
    }

    private fun cargar(reanudarReserva: Boolean) {
        carga?.cancel()
        _uiState.update { it.copy(carga = CargaOpciones.CARGANDO) }
        carga = viewModelScope.launch {
            when (val resultado = reservas.obtenerOpciones(torneoCodigo)) {
                is ResultadoOpciones.Disponibles -> {
                    mostrarOpciones(resultado)
                    if (reanudarReserva) reanudar()
                }
                ResultadoOpciones.SesionRequerida -> {
                    _uiState.update { it.copy(carga = CargaOpciones.SESION_REQUERIDA) }
                    // Sesión local con cookie vencida: se cierra para que el próximo login se note.
                    if (sesiones.sesion.value != null) sesiones.logout()
                    // Si faltaba enviar la reserva, eso sigue pendiente tras cargar las opciones.
                    if (_uiState.value.pendiente == null) pedirLogin(Pendiente.OPCIONES) else iniciarSesion()
                }
                ResultadoOpciones.SinConexion -> _uiState.update { it.copy(carga = CargaOpciones.SIN_CONEXION) }
                ResultadoOpciones.ErrorSitio -> _uiState.update { it.copy(carga = CargaOpciones.ERROR_SITIO) }
            }
        }
    }

    private fun mostrarOpciones(resultado: ResultadoOpciones.Disponibles) {
        val habilitadas = resultado.opciones.filter { it.habilitada }
        _uiState.update { estado ->
            val seleccionada = estado.seleccionada?.takeIf { elegida -> habilitadas.any { it.categoria == elegida } }
                // RNF08: con una sola categoría habilitada ya viene elegida.
                ?: habilitadas.singleOrNull()?.categoria
            estado.copy(
                carga = CargaOpciones.LISTA,
                opciones = resultado.opciones,
                seleccionada = seleccionada,
                pendiente = estado.pendiente.takeIf { it == Pendiente.RESERVA },
            )
        }
        guardado[CLAVE_SELECCIONADA] = _uiState.value.seleccionada
        guardado[CLAVE_PENDIENTE] = _uiState.value.pendiente?.name
    }

    /** Envía la reserva pendiente; si ya no se puede (p. ej. la categoría quedó deshabilitada), se descarta. */
    private fun reanudar() {
        if (_uiState.value.puedeConfirmar) {
            confirmar()
        } else {
            _uiState.update { it.copy(pendiente = null) }
            guardado[CLAVE_PENDIENTE] = null
        }
    }

    companion object {
        private const val CLAVE_SELECCIONADA = "reserva.seleccionada"
        private const val CLAVE_CONDICIONES = "reserva.condiciones"
        private const val CLAVE_PENDIENTE = "reserva.pendiente"

        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as TMTApplication
                val guardado = createSavedStateHandle()
                ReservaViewModel(
                    torneoCodigo = guardado.toRoute<Reserva>().torneoCodigo,
                    reservas = app.container.reservaRepository,
                    sesiones = app.container.sesionRepository,
                    conectividad = app.container.conectividad,
                    guardado = guardado,
                    envioSimulado = app.container.inscripcionesSimuladas,
                )
            }
        }
    }
}
