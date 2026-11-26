package com.uade.tmt.presentation.ranking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.toRoute
import com.uade.tmt.TMTApplication
import com.uade.tmt.domain.model.Jugador
import com.uade.tmt.domain.repository.JugadorRepository
import com.uade.tmt.presentation.navigation.JugadorFicha
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface FichaUiState {
    data object Cargando : FichaUiState

    data object NoEncontrado : FichaUiState

    data class Contenido(
        val jugador: Jugador,
        val seguido: Boolean,
        val esPropio: Boolean,
        /** Nombre del jugador que hoy es el perfil propio, si es otro. */
        val otroPropio: String? = null,
    ) : FichaUiState
}

/** RF03: ficha de un jugador. Todo sale de lo guardado, así que se ve sin conexión. */
class FichaJugadorViewModel(
    private val codigo: Int,
    private val repositorio: JugadorRepository,
) : ViewModel() {

    val uiState: StateFlow<FichaUiState> = combine(
        repositorio.observarJugador(codigo),
        repositorio.observarSeguidos(),
    ) { jugador, seguidos ->
        val marca = seguidos.firstOrNull { it.jugador.codigo == codigo }
        if (jugador == null) {
            FichaUiState.NoEncontrado
        } else {
            FichaUiState.Contenido(
                jugador,
                seguido = marca != null,
                esPropio = marca?.esPropio == true,
                otroPropio = seguidos.firstOrNull { it.esPropio && it.jugador.codigo != codigo }?.jugador?.nombre,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FichaUiState.Cargando)

    fun alternarSeguir() {
        val actual = uiState.value as? FichaUiState.Contenido ?: return
        viewModelScope.launch {
            if (actual.seguido) repositorio.dejarDeSeguir(codigo) else repositorio.seguir(codigo)
        }
    }

    /** Solo puede haber un perfil propio: el anterior queda como seguido común. */
    fun marcarComoPropio() {
        viewModelScope.launch {
            repositorio.observarSeguidos().first()
                .filter { it.esPropio && it.jugador.codigo != codigo }
                .forEach { repositorio.seguir(it.jugador.codigo, esPropio = false) }
            repositorio.seguir(codigo, esPropio = true)
        }
    }

    fun quitarPropio() {
        viewModelScope.launch { repositorio.seguir(codigo, esPropio = false) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as TMTApplication
                val ruta = createSavedStateHandle().toRoute<JugadorFicha>()
                FichaJugadorViewModel(ruta.codigo, app.container.jugadorRepository)
            }
        }
    }
}