package com.uade.tmt.presentation.clubes.detalle

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
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
import com.uade.tmt.domain.model.Club
import com.uade.tmt.domain.model.MotivoFallo
import com.uade.tmt.presentation.components.ErrorState
import com.uade.tmt.presentation.components.FalloBanner
import com.uade.tmt.presentation.components.LastUpdatedBanner
import com.uade.tmt.presentation.components.LoadingState
import com.uade.tmt.presentation.components.OfflineState
import com.uade.tmt.presentation.mensaje
import com.uade.tmt.presentation.theme.TMTTheme
import com.uade.tmt.presentation.torneos.Renglon

/**
 * Detalle de un club (RF04, G5): dirección, localidad, contacto y distancia. "Cómo llegar" y
 * "Llamar" delegan en las apps del teléfono con un Intent, sin permisos extra.
 */
@Composable
fun ClubDetalleScreen(
    onVolver: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ClubDetalleViewModel = viewModel(factory = ClubDetalleViewModel.Factory),
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    val conectado by viewModel.conectado.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LifecycleEventEffect(Lifecycle.Event.ON_START) { viewModel.actualizar(forzar = false) }
    LaunchedEffect(conectado) { if (conectado) viewModel.actualizar(forzar = false) }

    ClubDetalleContent(
        estado = estado,
        onVolver = onVolver,
        onActualizar = { viewModel.actualizar(forzar = true) },
        onComoLlegar = { abrirMapa(context, it) },
        onLlamar = { llamar(context, it) },
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClubDetalleContent(
    estado: ClubDetalleUiState,
    onVolver: () -> Unit,
    onActualizar: () -> Unit,
    onComoLlegar: (Club) -> Unit,
    onLlamar: (Club) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(stringResource(R.string.clubes_detalle_titulo)) },
            navigationIcon = {
                IconButton(onClick = onVolver) {
                    Icon(painterResource(R.drawable.ic_volver), stringResource(R.string.clubes_volver))
                }
            },
            // El Scaffold principal ya reserva el espacio de la barra de estado.
            windowInsets = WindowInsets(0),
        )
        when (estado) {
            ClubDetalleUiState.Cargando -> LoadingState(message = stringResource(R.string.estado_cargando))

            is ClubDetalleUiState.Error -> if (estado.motivo == MotivoFallo.SIN_CONEXION) {
                OfflineState(onRetry = onActualizar)
            } else {
                ErrorState(onRetry = onActualizar, message = stringResource(estado.motivo.mensaje))
            }

            is ClubDetalleUiState.Contenido -> {
                estado.actualizadoEn?.let {
                    LastUpdatedBanner(lastUpdatedMillis = it.toEpochMilli(), isOffline = estado.sinConexion)
                }
                // Sin ficha bajada no hay fecha que mostrar: la falta de conexión se avisa acá.
                if (estado.fallo != null && (estado.fallo != MotivoFallo.SIN_CONEXION || estado.actualizadoEn == null)) {
                    FalloBanner(motivo = estado.fallo, onRetry = onActualizar)
                }
                PullToRefreshBox(
                    isRefreshing = estado.actualizando,
                    onRefresh = onActualizar,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    DatosClub(estado, onComoLlegar, onLlamar)
                }
            }
        }
    }
}

@Composable
private fun DatosClub(
    estado: ClubDetalleUiState.Contenido,
    onComoLlegar: (Club) -> Unit,
    onLlamar: (Club) -> Unit,
) {
    val club = estado.club
    val distancia = estado.distancia
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_clubes),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
        Text(
            text = club.nombre,
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.semantics { heading() },
        )

        val direccion = listOfNotNull(club.direccion, club.localidad).joinToString(" · ")
        Renglon(R.drawable.ic_clubes) {
            Text(
                text = when {
                    club.direccion != null -> direccion
                    // La dirección viene en la ficha: si todavía no se bajó, no es que el sitio no la tenga.
                    estado.actualizadoEn == null && estado.actualizando ->
                        stringResource(R.string.clubes_detalle_buscando_ficha)
                    estado.actualizadoEn == null -> stringResource(R.string.clubes_detalle_sin_ficha)
                    else -> stringResource(R.string.clubes_detalle_sin_direccion)
                },
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        if (distancia != null) {
            Renglon(R.drawable.ic_cerca) {
                Text(
                    text = stringResource(R.string.clubes_a_distancia, distancia),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }

        // Una acción principal (Cómo llegar) y una secundaria (Llamar), al alcance del pulgar.
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = { onComoLlegar(club) },
                enabled = uriComoLlegar(club) != null,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp),
            ) {
                Icon(painterResource(R.drawable.ic_como_llegar), contentDescription = null, modifier = Modifier.size(18.dp))
                Text(stringResource(R.string.clubes_como_llegar), modifier = Modifier.padding(start = 8.dp))
            }
            if (uriLlamar(club) != null) {
                FilledTonalButton(
                    onClick = { onLlamar(club) },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp),
                ) {
                    Icon(painterResource(R.drawable.ic_llamar), contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(stringResource(R.string.clubes_llamar), modifier = Modifier.padding(start = 8.dp))
                }
            }
        }

        club.telefono?.let { Dato(stringResource(R.string.clubes_detalle_contacto), it) }
    }
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

/** Criterio 3: abre la app de mapas; si no hay ninguna, Google Maps en el navegador. */
private fun abrirMapa(context: Context, club: Club) {
    val geo = uriComoLlegar(club) ?: return
    if (!abrir(context, geo)) {
        val web = urlMapaWeb(club)
        if (web == null || !abrir(context, web)) avisar(context, R.string.clubes_sin_app_mapas)
    }
}

private fun llamar(context: Context, club: Club) {
    val tel = uriLlamar(club) ?: return
    // ACTION_DIAL solo abre el marcador con el número: no necesita el permiso CALL_PHONE.
    if (!abrir(context, tel, Intent.ACTION_DIAL)) avisar(context, R.string.clubes_sin_app_telefono)
}

private fun abrir(context: Context, uri: String, accion: String = Intent.ACTION_VIEW): Boolean = try {
    context.startActivity(Intent(accion, Uri.parse(uri)))
    true
} catch (e: ActivityNotFoundException) {
    false
}

private fun avisar(context: Context, @StringRes mensaje: Int) {
    Toast.makeText(context, mensaje, Toast.LENGTH_SHORT).show()
}

@PreviewLightDark
@Composable
private fun ClubDetallePreview() {
    TMTTheme {
        Surface {
            ClubDetalleContent(
                estado = ClubDetalleUiState.Contenido(
                    club = Club(
                        codigo = 81,
                        nombre = "Club de Muestra",
                        direccion = "Calle Inventada 123",
                        localidad = "Villa Ejemplo",
                        telefono = "11 0000-0000",
                        latitud = -34.6,
                        longitud = -58.4,
                    ),
                    distancia = "1,2 km",
                    actualizadoEn = null,
                    actualizando = false,
                    fallo = null,
                    sinConexion = false,
                ),
                onVolver = {},
                onActualizar = {},
                onComoLlegar = {},
                onLlamar = {},
            )
        }
    }
}
