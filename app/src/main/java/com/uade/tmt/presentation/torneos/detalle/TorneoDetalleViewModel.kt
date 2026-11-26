package com.uade.tmt.presentation.torneos.detalle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.toRoute
import com.uade.tmt.TMTApplication
import com.uade.tmt.domain.model.Inscripcion
import com.uade.tmt.domain.model.MotivoFallo
import com.uade.tmt.domain.model.Torneo
import com.uade.tmt.domain.repository.Conectividad
import com.uade.tmt.domain.repository.ReservaRepository
import com.uade.tmt.domain.repository.TorneoRepository
import com.uade.tmt.presentation.EstadoSync
import com.uade.tmt.presentation.aEstado
import com.uade.tmt.presentation.navigation.TorneoDetalle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant

sealed interface TorneoDetalleUiState {
    data object Cargando : TorneoDetalleUiState

    data class Error(val motivo: MotivoFallo) : TorneoDetalleUiState

    /** [actualizadoEn] es null si solo está lo que trae el calendario. */
    data class Contenido(
        val torneo: Torneo,
        val actualizadoEn: Instant?,
        val actualizando: Boolean,
        val fallo: MotivoFallo?,
        val sinConexion: Boolean,
        /** RF02: lo que el jugador ya reservó en este torneo desde la app. */
        val inscripciones: List<Inscripcion> = emptyList(),
    ) : TorneoDetalleUiState
}

class TorneoDetalleViewModel(
    private val codigo: Int,
    private val repositorio: TorneoRepository,
    conectividad: Conectividad,
    reservas: ReservaRepository,
) : ViewModel() {

    private val sync = MutableStateFlow(EstadoSync())

    val conectado: StateFlow<Boolean> = conectividad.conectado
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    val uiState: StateFlow<TorneoDetalleUiState> = combine(
        repositorio.observarTorneo(codigo),
        repositorio.observarActualizacionTorneo(codigo),
        sync,
        conectado,
        reservas.observarInscripciones().map { lista -> lista.filter { it.torneoCodigo == codigo } },
    ) { torneo, actualizadoEn, sync, conectado, inscripciones ->
        when {
            torneo != null -> TorneoDetalleUiState.Contenido(
                torneo = torneo,
                actualizadoEn = actualizadoEn,
                actualizando = sync.enCurso,
                fallo = sync.fallo,
                sinConexion = !conectado || sync.fallo == MotivoFallo.SIN_CONEXION,
                inscripciones = inscripciones,
            )

            sync.fallo != null -> TorneoDetalleUiState.Error(sync.fallo)
            else -> TorneoDetalleUiState.Cargando
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TorneoDetalleUiState.Cargando)

    fun actualizar(forzar: Boolean) {
        if (sync.value.enCurso) return
        sync.value = EstadoSync(enCurso = true)
        viewModelScope.launch {
            sync.value = repositorio.sincronizarTorneo(codigo, forzar).aEstado()
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as TMTApplication
                val ruta = createSavedStateHandle().toRoute<TorneoDetalle>()
                TorneoDetalleViewModel(
                    ruta.codigo,
                    app.container.torneoRepository,
                    app.container.conectividad,
                    app.container.reservaRepository,
                )
            }
        }
    }
}
