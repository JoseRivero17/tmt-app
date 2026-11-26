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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface FichaUiState {
    data object Cargando : FichaUiState

    data object NoEncontrado : FichaUiState

    data class Contenido(
        val jugador: Jugador,
        val seguido: Boolean,
        /** Es el jugador con sesión iniciada: se ve "Tu perfil" y no se lo sigue ni se lo deja de seguir. */
        val esPropio: Boolean,
    ) : FichaUiState
}

/** RF03: ficha de un jugador. Todo sale de lo guardado, así que se ve sin conexión. El perfil propio sale del login. */
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
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FichaUiState.Cargando)

    fun alternarSeguir() {
        val actual = uiState.value as? FichaUiState.Contenido ?: return
        if (actual.esPropio) return
        viewModelScope.launch {
            if (actual.seguido) repositorio.dejarDeSeguir(codigo) else repositorio.seguir(codigo)
        }
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