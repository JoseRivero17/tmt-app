package com.uade.tmt.presentation.perfil

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.uade.tmt.R
import com.uade.tmt.domain.model.EstadoInscripcion
import com.uade.tmt.domain.model.SesionJugador
import com.uade.tmt.presentation.components.EmptyState
import com.uade.tmt.presentation.components.LoadingState
import com.uade.tmt.presentation.theme.TMTTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Pestaña "Mi TMT" (RF02): sesión del jugador y sus inscripciones hechas desde la app. */
@Composable
fun PerfilScreen(
    onIniciarSesion: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PerfilViewModel = viewModel(factory = PerfilViewModel.Factory),
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    PerfilContent(
        estado = estado,
        onIniciarSesion = onIniciarSesion,
        onCerrarSesion = viewModel::cerrarSesion,
        modifier = modifier,
    )
}

@Composable
private fun PerfilContent(
    estado: PerfilUiState,
    onIniciarSesion: () -> Unit,
    onCerrarSesion: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sesion = estado.sesion
    if (sesion == null) {
        EmptyState(
            message = stringResource(R.string.perfil_sin_sesion),
            actionLabel = stringResource(R.string.perfil_iniciar_sesion),
            onAction = onIniciarSesion,
            modifier = modifier,
        )
        return
    }

    Column(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = sesion.nombre ?: sesion.codigo,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.semantics { heading() },
            )
            OutlinedButton(
                onClick = onCerrarSesion,
                modifier = Modifier.heightIn(min = 48.dp),
            ) { Text(stringResource(R.string.perfil_cerrar_sesion)) }
            Text(
                text = stringResource(R.string.perfil_mis_inscripciones),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() },
            )
        }
        val inscripciones = estado.inscripciones
        when {
            inscripciones == null -> LoadingState()
            inscripciones.isEmpty() -> EmptyState(message = stringResource(R.string.perfil_sin_inscripciones))
            else -> LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp)) {
                items(inscripciones, key = { "${it.torneoCodigo}-${it.categoria}" }) { FilaInscripcion(it) }
            }
        }
    }
}

@Composable
private fun FilaInscripcion(item: InscripcionItem) {
    val patron = stringResource(R.string.formato_fecha_inscripcion)
    val fecha = remember(item.registradaEn, patron) {
        DateTimeFormatter.ofPattern(patron).format(item.registradaEn.atZone(ZoneId.systemDefault()))
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {}
            .padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = item.torneo ?: stringResource(R.string.perfil_torneo_codigo, item.torneoCodigo),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = stringResource(
                R.string.perfil_inscripcion_detalle,
                item.categoria,
                stringResource(item.estado.etiqueta),
            ),
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = stringResource(R.string.perfil_inscripcion_fecha, fecha),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    HorizontalDivider()
}

@get:StringRes
private val EstadoInscripcion.etiqueta: Int
    get() = when (this) {
        EstadoInscripcion.RESERVADA -> R.string.perfil_estado_reservada
        EstadoInscripcion.LISTA_ESPERA -> R.string.perfil_estado_lista_espera
        EstadoInscripcion.NO_FIGURA -> R.string.perfil_estado_no_figura
    }

@PreviewLightDark
@Composable
private fun PerfilPreview() {
    TMTTheme {
        Surface {
            PerfilContent(
                estado = PerfilUiState(
                    sesion = SesionJugador("12345", "Jugador Demo"),
                    inscripciones = listOf(
                        InscripcionItem(1, "Torneo de prueba", "6ta", EstadoInscripcion.RESERVADA, Instant.EPOCH),
                        InscripcionItem(2, null, "7ma", EstadoInscripcion.LISTA_ESPERA, Instant.EPOCH),
                    ),
                ),
                onIniciarSesion = {},
                onCerrarSesion = {},
            )
        }
    }
}
