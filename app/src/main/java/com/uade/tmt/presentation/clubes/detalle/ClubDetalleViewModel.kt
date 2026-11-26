package com.uade.tmt.presentation.clubes.detalle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.toRoute
import com.uade.tmt.TMTApplication
import com.uade.tmt.domain.model.Club
import com.uade.tmt.domain.model.MotivoFallo
import com.uade.tmt.domain.repository.ClubRepository
import com.uade.tmt.domain.repository.Conectividad
import com.uade.tmt.presentation.EstadoSync
import com.uade.tmt.presentation.aEstado
import com.uade.tmt.presentation.navigation.ClubDetalle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant

sealed interface ClubDetalleUiState {
    data object Cargando : ClubDetalleUiState

    data class Error(val motivo: MotivoFallo) : ClubDetalleUiState

    /** [actualizadoEn] es null si todavía no se bajó la ficha: solo está lo que trae el listado. */
    data class Contenido(
        val club: Club,
        val distancia: String?,
        val actualizadoEn: Instant?,
        val actualizando: Boolean,
        val fallo: MotivoFallo?,
        val sinConexion: Boolean,
    ) : ClubDetalleUiState
}

/** RF04: detalle de un club (G5). Muestra lo guardado y baja la ficha con la dirección y el contacto. */
class ClubDetalleViewModel(
    private val codigo: Int,
    private val distancia: String?,
    private val repositorio: ClubRepository,
    conectividad: Conectividad,
) : ViewModel() {

    private val sync = MutableStateFlow(EstadoSync())

    val conectado: StateFlow<Boolean> = conectividad.conectado
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    val uiState: StateFlow<ClubDetalleUiState> = combine(
        repositorio.observarClub(codigo),
        repositorio.observarActualizacionClub(codigo),
        sync,
        conectado,
    ) { club, actualizadoEn, sync, conectado ->
        when {
            club != null -> ClubDetalleUiState.Contenido(
                club = club,
                distancia = distancia,
                actualizadoEn = actualizadoEn,
                actualizando = sync.enCurso,
                fallo = sync.fallo,
                sinConexion = !conectado || sync.fallo == MotivoFallo.SIN_CONEXION,
            )

            sync.fallo != null -> ClubDetalleUiState.Error(sync.fallo)
            else -> ClubDetalleUiState.Cargando
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ClubDetalleUiState.Cargando)

    fun actualizar(forzar: Boolean) {
        if (sync.value.enCurso) return
        sync.value = EstadoSync(enCurso = true)
        viewModelScope.launch {
            sync.value = repositorio.sincronizarClub(codigo, forzar).aEstado()
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as TMTApplication
                val ruta = createSavedStateHandle().toRoute<ClubDetalle>()
                ClubDetalleViewModel(ruta.codigo, ruta.distancia, app.container.clubRepository, app.container.conectividad)
            }
        }
    }
}
