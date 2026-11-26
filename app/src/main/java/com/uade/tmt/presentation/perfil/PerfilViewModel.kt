package com.uade.tmt.presentation.perfil

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.uade.tmt.TMTApplication
import com.uade.tmt.domain.model.EstadoInscripcion
import com.uade.tmt.domain.model.Jugador
import com.uade.tmt.domain.model.MotivoFallo
import com.uade.tmt.domain.model.SesionJugador
import com.uade.tmt.domain.repository.JugadorRepository
import com.uade.tmt.domain.repository.ReservaRepository
import com.uade.tmt.domain.repository.SesionRepository
import com.uade.tmt.domain.repository.TorneoRepository
import com.uade.tmt.domain.usecase.CerrarSesion
import com.uade.tmt.presentation.EstadoSync
import com.uade.tmt.presentation.aEstado
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant

/** Inscripción lista para mostrar: el nombre sale de los torneos guardados, si sigue en el calendario. */
data class InscripcionItem(
    val torneoCodigo: Int,
    val torneo: String?,
    val categoria: String,
    val estado: EstadoInscripcion,
    val registradaEn: Instant,
)

data class PerfilUiState(
    val sesion: SesionJugador? = null,
    /** Ficha del jugador con sesión iniciada (RF03), o null si todavía no se bajó. */
    val ficha: Jugador? = null,
    val actualizandoFicha: Boolean = false,
    val falloFicha: MotivoFallo? = null,
    /** null mientras se lee el registro local. */
    val inscripciones: List<InscripcionItem>? = null,
)

/** RF02, pestaña "Mi TMT": la ficha de quien inició sesión y sus inscripciones. No recibe Context. */
@OptIn(ExperimentalCoroutinesApi::class)
class PerfilViewModel(
    private val sesiones: SesionRepository,
    private val reservas: ReservaRepository,
    torneos: TorneoRepository,
    private val jugadores: JugadorRepository,
    scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
) : ViewModel(scope) {

    private val cerrarSesionUseCase = CerrarSesion(sesiones, jugadores, reservas)
    private val syncFicha = MutableStateFlow(EstadoSync())

    /** Sale de lo guardado, así que se ve sin conexión. */
    private val ficha = sesiones.sesion.flatMapLatest { sesion ->
        sesion?.codigo?.toIntOrNull()?.let(jugadores::observarJugador) ?: flowOf(null)
    }

    val uiState: StateFlow<PerfilUiState> = combine(
        sesiones.sesion,
        ficha,
        syncFicha,
        reservas.observarInscripciones(),
        torneos.observarCalendario(),
    ) { sesion, ficha, sync, inscripciones, calendario ->
        val nombres = calendario.associate { it.codigo to it.nombre }
        PerfilUiState(
            sesion = sesion,
            ficha = ficha,
            actualizandoFicha = sync.enCurso,
            falloFicha = sync.fallo,
            inscripciones = inscripciones.map {
                InscripcionItem(it.torneoCodigo, nombres[it.torneoCodigo], it.categoria, it.estado, it.registradaEn)
            },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PerfilUiState(sesion = sesiones.sesion.value))

    /** Con [forzar] en false solo consulta el sitio si la ficha guardada venció (RNF05). */
    fun actualizarFicha(forzar: Boolean) {
        val codigo = sesiones.sesion.value?.codigo?.toIntOrNull() ?: return
        if (syncFicha.value.enCurso) return
        syncFicha.value = EstadoSync(enCurso = true)
        viewModelScope.launch {
            syncFicha.value = jugadores.sincronizarFicha(codigo, forzar).aEstado()
        }
    }

    fun cerrarSesion() {
        syncFicha.value = EstadoSync()
        viewModelScope.launch { cerrarSesionUseCase() }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as TMTApplication
                PerfilViewModel(
                    app.container.sesionRepository,
                    app.container.reservaRepository,
                    app.container.torneoRepository,
                    app.container.jugadorRepository,
                )
            }
        }
    }
}
