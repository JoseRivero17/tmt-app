package com.uade.tmt.presentation.perfil

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.uade.tmt.TMTApplication
import com.uade.tmt.domain.model.EstadoInscripcion
import com.uade.tmt.domain.model.SesionJugador
import com.uade.tmt.domain.repository.ReservaRepository
import com.uade.tmt.domain.repository.SesionRepository
import com.uade.tmt.domain.repository.TorneoRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
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
    /** null mientras se lee el registro local. */
    val inscripciones: List<InscripcionItem>? = null,
)

/** RF02, pestaña "Mi TMT". No recibe Context. */
class PerfilViewModel(
    private val sesiones: SesionRepository,
    reservas: ReservaRepository,
    torneos: TorneoRepository,
    scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
) : ViewModel(scope) {

    val uiState: StateFlow<PerfilUiState> = combine(
        sesiones.sesion,
        reservas.observarInscripciones(),
        torneos.observarCalendario(),
    ) { sesion, inscripciones, calendario ->
        val nombres = calendario.associate { it.codigo to it.nombre }
        PerfilUiState(
            sesion = sesion,
            inscripciones = inscripciones.map {
                InscripcionItem(it.torneoCodigo, nombres[it.torneoCodigo], it.categoria, it.estado, it.registradaEn)
            },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PerfilUiState(sesion = sesiones.sesion.value))

    fun cerrarSesion() {
        viewModelScope.launch { sesiones.logout() }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as TMTApplication
                PerfilViewModel(
                    app.container.sesionRepository,
                    app.container.reservaRepository,
                    app.container.torneoRepository,
                )
            }
        }
    }
}
