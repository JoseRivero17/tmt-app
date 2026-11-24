package com.uade.tmt.presentation.torneos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.uade.tmt.TMTApplication
import com.uade.tmt.domain.model.FiltrosTorneo
import com.uade.tmt.domain.model.MotivoFallo
import com.uade.tmt.domain.model.Torneo
import com.uade.tmt.domain.repository.Conectividad
import com.uade.tmt.domain.repository.TorneoRepository
import com.uade.tmt.domain.usecase.FiltrarTorneos
import com.uade.tmt.presentation.EstadoSync
import com.uade.tmt.presentation.aEstado
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant

class TorneosViewModel(
    private val repositorio: TorneoRepository,
    conectividad: Conectividad,
    private val filtrar: FiltrarTorneos = FiltrarTorneos(),
) : ViewModel() {

    private val filtros = MutableStateFlow(FiltrosTorneo())
    private val sync = MutableStateFlow(EstadoSync())

    val conectado: StateFlow<Boolean> = conectividad.conectado
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    /** Combina lo guardado, los filtros y la actualización: si cambia cualquiera, se redibuja. */
    val uiState: StateFlow<TorneosUiState> = combine(
        repositorio.observarCalendario(),
        repositorio.observarActualizacionCalendario(),
        filtros,
        sync,
        conectado,
        ::armarEstado,
    ).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TorneosUiState.Cargando)

    /** Con [forzar] en false solo consulta el sitio si lo guardado venció (RNF05). */
    fun actualizar(forzar: Boolean) {
        if (sync.value.enCurso) return
        sync.value = EstadoSync(enCurso = true)
        viewModelScope.launch {
            sync.value = repositorio.sincronizarCalendario(forzar).aEstado()
        }
    }

    fun filtrarPor(nuevos: FiltrosTorneo) {
        filtros.value = nuevos
    }

    fun limpiarFiltros() = filtrarPor(FiltrosTorneo())

    private fun armarEstado(
        torneos: List<Torneo>,
        actualizadoEn: Instant?,
        filtros: FiltrosTorneo,
        sync: EstadoSync,
        conectado: Boolean,
    ): TorneosUiState = when {
        actualizadoEn != null -> TorneosUiState.Contenido(
            dias = filtrar(torneos, filtros)
                .groupBy { it.fecha }
                .map { (fecha, delDia) -> DiaTorneos(fecha, delDia) },
            filtros = filtros,
            opciones = OpcionesFiltro(
                divisiones = torneos.flatMap { it.categorias }.map { it.division }.distinct().sorted(),
                localidades = torneos.map { it.localidad }.distinct().sorted(),
            ),
            actualizadoEn = actualizadoEn,
            actualizando = sync.enCurso,
            fallo = sync.fallo.takeIf { it != MotivoFallo.SIN_CONEXION },
            sinConexion = !conectado || sync.fallo == MotivoFallo.SIN_CONEXION,
        )

        sync.fallo != null -> TorneosUiState.Error(sync.fallo)
        else -> TorneosUiState.Cargando
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as TMTApplication
                TorneosViewModel(app.container.torneoRepository, app.container.conectividad)
            }
        }
    }
}
