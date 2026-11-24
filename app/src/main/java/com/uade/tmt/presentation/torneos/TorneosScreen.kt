package com.uade.tmt.presentation.torneos

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.uade.tmt.R
import com.uade.tmt.domain.model.CategoriaTorneo
import com.uade.tmt.domain.model.FiltrosTorneo
import com.uade.tmt.domain.model.MotivoFallo
import com.uade.tmt.domain.model.Torneo
import com.uade.tmt.presentation.components.EmptyState
import com.uade.tmt.presentation.components.ErrorState
import com.uade.tmt.presentation.components.FalloBanner
import com.uade.tmt.presentation.components.LastUpdatedBanner
import com.uade.tmt.presentation.components.LoadingState
import com.uade.tmt.presentation.components.OfflineState
import com.uade.tmt.presentation.mensaje
import com.uade.tmt.presentation.theme.TMTTheme
import java.time.Instant
import java.time.LocalDate

/** Calendario de torneos (RF01): pantalla de inicio de la app. */
@Composable
fun TorneosScreen(
    onTorneoClick: (codigo: Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TorneosViewModel = viewModel(factory = TorneosViewModel.Factory),
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    val conectado by viewModel.conectado.collectAsStateWithLifecycle()
    // También al volver a la app: el intervalo mínimo evita pedidos de más (RNF05).
    LifecycleEventEffect(Lifecycle.Event.ON_START) { viewModel.actualizar(forzar = false) }
    // Y al recuperar la conexión, solo con la pantalla a la vista (RNF12).
    LaunchedEffect(conectado) { if (conectado) viewModel.actualizar(forzar = false) }

    TorneosContent(
        estado = estado,
        onTorneoClick = onTorneoClick,
        onActualizar = { viewModel.actualizar(forzar = true) },
        onFiltrar = viewModel::filtrarPor,
        onLimpiarFiltros = viewModel::limpiarFiltros,
        modifier = modifier,
    )
}

@Composable
private fun TorneosContent(
    estado: TorneosUiState,
    onTorneoClick: (Int) -> Unit,
    onActualizar: () -> Unit,
    onFiltrar: (FiltrosTorneo) -> Unit,
    onLimpiarFiltros: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        Text(
            text = stringResource(R.string.torneos_titulo),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .semantics { heading() },
        )
        when (estado) {
            TorneosUiState.Cargando -> LoadingState(message = stringResource(R.string.estado_cargando))

            is TorneosUiState.Error -> if (estado.motivo == MotivoFallo.SIN_CONEXION) {
                OfflineState(onRetry = onActualizar)
            } else {
                ErrorState(onRetry = onActualizar, message = stringResource(estado.motivo.mensaje))
            }

            is TorneosUiState.Contenido -> Calendario(
                estado = estado,
                onTorneoClick = onTorneoClick,
                onActualizar = onActualizar,
                onFiltrar = onFiltrar,
                onLimpiarFiltros = onLimpiarFiltros,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun Calendario(
    estado: TorneosUiState.Contenido,
    onTorneoClick: (Int) -> Unit,
    onActualizar: () -> Unit,
    onFiltrar: (FiltrosTorneo) -> Unit,
    onLimpiarFiltros: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        FiltrosTorneos(
            filtros = estado.filtros,
            opciones = estado.opciones,
            onFiltrar = onFiltrar,
            onLimpiar = onLimpiarFiltros,
        )
        LastUpdatedBanner(
            lastUpdatedMillis = estado.actualizadoEn.toEpochMilli(),
            isOffline = estado.sinConexion,
        )
        if (estado.fallo != null) {
            FalloBanner(motivo = estado.fallo, onRetry = onActualizar)
        }
        PullToRefreshBox(
            isRefreshing = estado.actualizando,
            onRefresh = onActualizar,
            modifier = Modifier.fillMaxSize(),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (estado.dias.isEmpty()) {
                    item {
                        SinTorneos(
                            hayFiltros = estado.filtros.hayAlguno,
                            onLimpiarFiltros = onLimpiarFiltros,
                            modifier = Modifier.fillParentMaxSize(),
                        )
                    }
                }
                estado.dias.forEach { dia ->
                    stickyHeader(key = dia.fecha.toString()) { EncabezadoDia(dia.fecha) }
                    items(dia.torneos, key = { it.codigo }) { torneo ->
                        TorneoCard(
                            torneo = torneo,
                            divisionElegida = estado.filtros.division,
                            onClick = { onTorneoClick(torneo.codigo) },
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SinTorneos(hayFiltros: Boolean, onLimpiarFiltros: () -> Unit, modifier: Modifier = Modifier) {
    if (hayFiltros) {
        EmptyState(
            message = stringResource(R.string.torneos_sin_coincidencias),
            actionLabel = stringResource(R.string.estado_limpiar_filtros),
            onAction = onLimpiarFiltros,
            modifier = modifier,
        )
    } else {
        EmptyState(message = stringResource(R.string.torneos_sin_proximos), modifier = modifier)
    }
}

@Composable
private fun EncabezadoDia(fecha: LocalDate) {
    Surface(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface) {
        Text(
            text = fecha.larga(),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .semantics { heading() },
        )
    }
}

internal val TorneoDePrueba = Torneo(
    codigo = 1,
    nombre = "10° Torneo \"Copa de Muestra\"",
    fecha = LocalDate.of(2026, 11, 27),
    localidad = "Villa Ejemplo",
    sede = "Club de Muestra",
    direccion = "Calle Inventada 123",
    inscripcionOnline = true,
    precios = "$1000 inscripción general",
    categorias = listOf(
        CategoriaTorneo("7ma", "3 sets, final 5 sets", "20:30", 18),
        CategoriaTorneo("8va", "3 sets, final 5 sets", "21:00", 12),
    ),
)

@PreviewLightDark
@Composable
private fun TorneosPreview() {
    TMTTheme(dynamicColor = false) {
        Surface {
            TorneosContent(
                estado = TorneosUiState.Contenido(
                    dias = listOf(
                        DiaTorneos(
                            TorneoDePrueba.fecha,
                            listOf(TorneoDePrueba, TorneoDePrueba.copy(codigo = 2, cancelado = true)),
                        ),
                    ),
                    filtros = FiltrosTorneo(division = "7ma"),
                    opciones = OpcionesFiltro(listOf("7ma"), listOf("Villa Ejemplo")),
                    actualizadoEn = Instant.EPOCH,
                    actualizando = false,
                    fallo = null,
                    sinConexion = false,
                ),
                onTorneoClick = {},
                onActualizar = {},
                onFiltrar = {},
                onLimpiarFiltros = {},
            )
        }
    }
}
