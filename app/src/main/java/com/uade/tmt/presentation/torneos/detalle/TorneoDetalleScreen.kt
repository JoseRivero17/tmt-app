package com.uade.tmt.presentation.torneos.detalle

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
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
import com.uade.tmt.domain.model.MotivoFallo
import com.uade.tmt.domain.model.Torneo
import com.uade.tmt.presentation.components.ErrorState
import com.uade.tmt.presentation.components.FalloBanner
import com.uade.tmt.presentation.components.LastUpdatedBanner
import com.uade.tmt.presentation.components.LoadingState
import com.uade.tmt.presentation.components.OfflineState
import com.uade.tmt.presentation.mensaje
import com.uade.tmt.presentation.theme.TMTTheme
import com.uade.tmt.presentation.torneos.EstadoTorneo
import com.uade.tmt.presentation.torneos.Renglon
import com.uade.tmt.presentation.torneos.TorneoDePrueba
import com.uade.tmt.presentation.torneos.larga

/** Detalle de un torneo (RF01). RF02 agrega acá la reserva, según [Torneo.admiteReserva]. */
@Composable
fun TorneoDetalleScreen(
    onVolver: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TorneoDetalleViewModel = viewModel(factory = TorneoDetalleViewModel.Factory),
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    val conectado by viewModel.conectado.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_START) { viewModel.actualizar(forzar = false) }
    LaunchedEffect(conectado) { if (conectado) viewModel.actualizar(forzar = false) }

    TorneoDetalleContent(
        estado = estado,
        onVolver = onVolver,
        onActualizar = { viewModel.actualizar(forzar = true) },
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TorneoDetalleContent(
    estado: TorneoDetalleUiState,
    onVolver: () -> Unit,
    onActualizar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(stringResource(R.string.torneos_detalle_titulo)) },
            navigationIcon = {
                IconButton(onClick = onVolver) {
                    Icon(painterResource(R.drawable.ic_volver), stringResource(R.string.torneos_volver))
                }
            },
            // El Scaffold principal ya reserva el espacio de la barra de estado.
            windowInsets = WindowInsets(0),
        )
        when (estado) {
            TorneoDetalleUiState.Cargando -> LoadingState(message = stringResource(R.string.estado_cargando))

            is TorneoDetalleUiState.Error -> if (estado.motivo == MotivoFallo.SIN_CONEXION) {
                OfflineState(onRetry = onActualizar)
            } else {
                ErrorState(onRetry = onActualizar, message = stringResource(estado.motivo.mensaje))
            }

            is TorneoDetalleUiState.Contenido -> {
                estado.actualizadoEn?.let {
                    LastUpdatedBanner(
                        lastUpdatedMillis = it.toEpochMilli(),
                        isOffline = estado.sinConexion,
                    )
                }
                // Sin detalle bajado no hay fecha que mostrar: la falta de conexión se avisa acá.
                if (estado.fallo != null && (estado.fallo != MotivoFallo.SIN_CONEXION || estado.actualizadoEn == null)) {
                    FalloBanner(motivo = estado.fallo, onRetry = onActualizar)
                }
                PullToRefreshBox(
                    isRefreshing = estado.actualizando,
                    onRefresh = onActualizar,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    DatosTorneo(estado.torneo)
                }
            }
        }
    }
}

@Composable
private fun DatosTorneo(torneo: Torneo) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        EstadoTorneo(torneo)
        Text(
            text = torneo.nombre,
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.semantics { heading() },
        )
        Renglon(R.drawable.ic_fecha) {
            Text(text = torneo.fecha.larga(), style = MaterialTheme.typography.bodyLarge)
        }
        Renglon(R.drawable.ic_clubes) {
            Text(
                text = torneo.sede?.let { listOfNotNull(it, torneo.direccion).joinToString("\n") } ?: torneo.localidad,
                style = MaterialTheme.typography.bodyLarge,
            )
        }

        if (torneo.cancelado) {
            Text(
                text = stringResource(R.string.torneos_aviso_cancelado),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error,
            )
        }

        torneo.club?.let { Dato(stringResource(R.string.torneos_detalle_club), it) }

        Seccion(stringResource(R.string.torneos_detalle_divisiones))
        torneo.categorias.forEach { FilaDivision(it) }

        torneo.precios?.let { Dato(stringResource(R.string.torneos_detalle_precios), it) }
    }
}

@Composable
private fun Seccion(titulo: String) {
    Text(
        text = titulo,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.semantics { heading() },
    )
}

@Composable
private fun Dato(etiqueta: String, valor: String) {
    Column(modifier = Modifier.semantics(mergeDescendants = true) {}) {
        Text(
            text = etiqueta,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(text = valor, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun FilaDivision(categoria: CategoriaTorneo) {
    Column(modifier = Modifier.semantics(mergeDescendants = true) {}) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.torneos_detalle_division, categoria.division),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            categoria.hora?.let {
                Text(
                    text = stringResource(R.string.torneos_detalle_hora, it),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
        val detalle = listOfNotNull(
            categoria.detalle,
            categoria.cupos?.let { pluralStringResource(R.plurals.torneos_detalle_cupos, it, it) },
        )
        if (detalle.isNotEmpty()) {
            Text(
                text = detalle.joinToString(" · "),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
    }
}

@PreviewLightDark
@Composable
private fun TorneoDetallePreview() {
    TMTTheme {
        Surface {
            TorneoDetalleContent(
                estado = TorneoDetalleUiState.Contenido(TorneoDePrueba, null, false, MotivoFallo.TIMEOUT, false),
                onVolver = {},
                onActualizar = {},
            )
        }
    }
}
