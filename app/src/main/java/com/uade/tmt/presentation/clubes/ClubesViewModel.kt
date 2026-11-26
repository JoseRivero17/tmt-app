package com.uade.tmt.presentation.clubes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.uade.tmt.TMTApplication
import com.uade.tmt.domain.model.Club
import com.uade.tmt.domain.model.MotivoFallo
import com.uade.tmt.domain.model.Ubicacion
import com.uade.tmt.domain.repository.ClubRepository
import com.uade.tmt.domain.repository.Conectividad
import com.uade.tmt.domain.repository.UbicacionProvider
import com.uade.tmt.domain.usecase.BuscarClubes
import com.uade.tmt.domain.usecase.OrdenarClubesPorCercania
import com.uade.tmt.presentation.EstadoSync
import com.uade.tmt.presentation.aEstado
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant

/**
 * RF04. Combina los clubes guardados (Room) con la ubicación del usuario y el texto buscado.
 *
 * La ubicación se lee una vez cuando hay permiso y queda solo en memoria, mientras viva la
 * pantalla: no se guarda ni se envía (RNF07). No recibe Context: el permiso lo pide la pantalla
 * y le avisa el resultado. [scope] solo se cambia en los tests, que no tienen `Dispatchers.Main`.
 */
class ClubesViewModel(
    private val repositorio: ClubRepository,
    conectividad: Conectividad,
    private val ubicacionProvider: UbicacionProvider,
    private val ordenar: OrdenarClubesPorCercania = OrdenarClubesPorCercania(),
    private val buscar: BuscarClubes = BuscarClubes(),
    scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
) : ViewModel(scope) {

    /** Lo que eligió el usuario en esta pantalla. */
    private data class Consulta(
        val acceso: AccesoUbicacion = AccesoUbicacion.SIN_DECIDIR,
        val ubicacion: Ubicacion? = null,
        val ubicando: Boolean = false,
        val ubicacionLeida: Boolean = false,
        val busqueda: String = "",
    )

    private val consulta = MutableStateFlow(Consulta())
    private val sync = MutableStateFlow(EstadoSync())

    val conectado: StateFlow<Boolean> = conectividad.conectado
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    val uiState: StateFlow<ClubesUiState> = combine(
        repositorio.observarClubes(),
        repositorio.observarActualizacion(),
        consulta,
        sync,
        conectado,
        ::armarEstado,
    ).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ClubesUiState.Cargando)

    /** Con [forzar] en false solo consulta el sitio si lo guardado venció (RNF05). */
    fun actualizar(forzar: Boolean) {
        if (sync.value.enCurso) return
        sync.value = EstadoSync(enCurso = true)
        viewModelScope.launch {
            sync.value = repositorio.sincronizar(forzar).aEstado()
        }
    }

    /**
     * La pantalla avisa si hay permiso cada vez que se muestra: así se entera si el usuario lo dio
     * o lo quitó desde Ajustes. Si ya hay ubicación no se vuelve a leer.
     */
    fun revisarPermiso(concedido: Boolean) {
        val actual = consulta.value
        when {
            concedido && actual.acceso != AccesoUbicacion.CONCEDIDO -> permisoConcedido()
            !concedido && actual.acceso == AccesoUbicacion.CONCEDIDO -> permisoRechazado()
        }
    }

    /** Resultado del diálogo del sistema (G2) con el permiso concedido. */
    fun permisoConcedido() {
        consulta.update { it.copy(acceso = AccesoUbicacion.CONCEDIDO) }
        leerUbicacion()
    }

    fun permisoRechazado() {
        consulta.update { it.copy(acceso = AccesoUbicacion.RECHAZADO, ubicacion = null, ubicando = false) }
    }

    /** G1: "Buscar por localidad", sin pedir el permiso. */
    fun continuarSinUbicacion() {
        consulta.update { it.copy(acceso = AccesoUbicacion.SIN_UBICACION) }
    }

    fun buscar(texto: String) {
        consulta.update { it.copy(busqueda = texto) }
    }

    /** Una sola lectura (RNF07). También sirve para reintentar si el teléfono no dio la ubicación. */
    fun leerUbicacion() {
        if (consulta.value.ubicando) return
        consulta.update { it.copy(ubicando = true) }
        viewModelScope.launch {
            val ubicacion = ubicacionProvider.ubicacionActual()
            consulta.update { it.copy(ubicacion = ubicacion, ubicando = false, ubicacionLeida = true) }
        }
    }

    private fun armarEstado(
        clubes: List<Club>,
        actualizadoEn: Instant?,
        consulta: Consulta,
        sync: EstadoSync,
        conectado: Boolean,
    ): ClubesUiState = when {
        consulta.acceso == AccesoUbicacion.SIN_DECIDIR -> ClubesUiState.PedirPermiso

        actualizadoEn != null -> ClubesUiState.Contenido(
            clubes = ordenar(buscar(clubes, consulta.busqueda), consulta.ubicacion),
            busqueda = consulta.busqueda,
            acceso = consulta.acceso,
            ubicando = consulta.ubicando,
            sinUbicacion = consulta.acceso == AccesoUbicacion.CONCEDIDO &&
                consulta.ubicacionLeida && !consulta.ubicando && consulta.ubicacion == null,
            actualizadoEn = actualizadoEn,
            actualizando = sync.enCurso,
            fallo = sync.fallo.takeIf { it != MotivoFallo.SIN_CONEXION },
            sinConexion = !conectado || sync.fallo == MotivoFallo.SIN_CONEXION,
        )

        sync.fallo != null -> ClubesUiState.Error(sync.fallo)
        else -> ClubesUiState.Cargando
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as TMTApplication
                ClubesViewModel(
                    app.container.clubRepository,
                    app.container.conectividad,
                    app.container.ubicacion,
                )
            }
        }
    }
}
