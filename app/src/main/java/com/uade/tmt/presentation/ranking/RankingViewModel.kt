package com.uade.tmt.presentation.ranking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.uade.tmt.TMTApplication
import com.uade.tmt.domain.model.DIVISIONES
import com.uade.tmt.domain.model.Jugador
import com.uade.tmt.domain.model.MotivoFallo
import com.uade.tmt.domain.model.Seguido
import com.uade.tmt.domain.repository.Conectividad
import com.uade.tmt.domain.repository.JugadorRepository
import com.uade.tmt.presentation.EstadoSync
import com.uade.tmt.presentation.aEstado
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant

/** La búsqueda arranca recién con esta cantidad de letras (RF03). */
const val MIN_BUSQUEDA = 3

enum class PestanaJugadores { RANKING, SEGUIDOS }

data class RankingUiState(
    val pestana: PestanaJugadores = PestanaJugadores.RANKING,
    val division: String = DIVISIONES.first(),
    val busqueda: String = "",
    val jugadores: List<Jugador> = emptyList(),
    /** Jugadores que sigue el usuario; el perfil propio va primero. Salen de lo guardado. */
    val seguidos: List<Seguido> = emptyList(),
    val actualizadoEn: Instant? = null,
    val actualizando: Boolean = false,
    val fallo: MotivoFallo? = null,
    val sinConexion: Boolean = false,
) {
    val buscando: Boolean get() = busqueda.trim().length >= MIN_BUSQUEDA
}

/** RF03: ranking por división, búsqueda de jugadores y seguidos. */
@OptIn(ExperimentalCoroutinesApi::class)
class RankingViewModel(
    private val repositorio: JugadorRepository,
    conectividad: Conectividad,
) : ViewModel() {

    private val pestana = MutableStateFlow(PestanaJugadores.RANKING)
    private val division = MutableStateFlow(DIVISIONES.first())
    private val busqueda = MutableStateFlow("")
    private val sync = MutableStateFlow(EstadoSync())

    val conectado: StateFlow<Boolean> = conectividad.conectado
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    /** Con 3 letras o más busca en todos; si no, muestra el ranking de la división elegida. */
    private val jugadores: Flow<List<Jugador>> = combine(division, busqueda) { d, b -> d to b.trim() }
        .flatMapLatest { (d, b) ->
            if (b.length >= MIN_BUSQUEDA) repositorio.buscar(b) else repositorio.observarRanking(d)
        }

    private val actualizadoEn: Flow<Instant?> =
        division.flatMapLatest { repositorio.observarActualizacionRanking(it) }

    val uiState: StateFlow<RankingUiState> = combine(
        combine(pestana, division, busqueda, ::Triple),
        jugadores,
        repositorio.observarSeguidos(),
        actualizadoEn,
        combine(sync, conectado, ::Pair),
    ) { (p, d, b), lista, seguidos, actualizado, (s, con) ->
        RankingUiState(
            pestana = p,
            division = d,
            busqueda = b,
            jugadores = lista,
            seguidos = seguidos,
            actualizadoEn = actualizado,
            actualizando = s.enCurso,
            fallo = s.fallo.takeIf { it != MotivoFallo.SIN_CONEXION },
            sinConexion = !con || s.fallo == MotivoFallo.SIN_CONEXION,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RankingUiState())

    fun elegirPestana(nueva: PestanaJugadores) {
        pestana.value = nueva
    }

    fun buscar(texto: String) {
        busqueda.value = texto
    }

    fun elegirDivision(nueva: String) {
        division.value = nueva
        actualizar(forzar = false)
    }

    /** Con [forzar] en false solo consulta el sitio si lo guardado venció (RNF05). */
    fun actualizar(forzar: Boolean) {
        if (sync.value.enCurso) return
        sync.value = EstadoSync(enCurso = true)
        viewModelScope.launch {
            sync.value = repositorio.sincronizarRanking(division.value, forzar).aEstado()
        }
    }

    /** Actualiza las divisiones de los jugadores seguidos. */
    fun actualizarSeguidos() {
        if (sync.value.enCurso) return
        val divisiones = uiState.value.seguidos.map { it.jugador.division }.distinct()
        if (divisiones.isEmpty()) return
        sync.value = EstadoSync(enCurso = true)
        viewModelScope.launch {
            var resultado = EstadoSync()
            for (d in divisiones) {
                val intento = repositorio.sincronizarRanking(d, forzar = true).aEstado()
                if (intento.fallo != null) {
                    resultado = intento
                    break
                }
            }
            sync.value = resultado
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as TMTApplication
                RankingViewModel(app.container.jugadorRepository, app.container.conectividad)
            }
        }
    }
}