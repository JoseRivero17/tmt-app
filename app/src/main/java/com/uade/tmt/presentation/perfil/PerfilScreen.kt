package com.uade.tmt.presentation.perfil

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
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
import com.uade.tmt.domain.model.EstadoInscripcion
import com.uade.tmt.domain.model.Jugador
import com.uade.tmt.domain.model.SesionJugador
import com.uade.tmt.presentation.components.EmptyState
import com.uade.tmt.presentation.mensaje
import com.uade.tmt.presentation.components.LoadingState
import com.uade.tmt.presentation.theme.TMTTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Pestaña "Mi TMT" (RF02): la ficha de quien inició sesión y sus inscripciones hechas desde la app. */
@Composable
fun PerfilScreen(
    onIniciarSesion: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PerfilViewModel = viewModel(factory = PerfilViewModel.Factory),
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    // También al volver del login: la ficha propia se baja recién ahí, así el login no espera.
    LifecycleEventEffect(Lifecycle.Event.ON_START) { viewModel.actualizarFicha(forzar = false) }
    PerfilContent(
        estado = estado,
        onIniciarSesion = onIniciarSesion,
        onCerrarSesion = viewModel::cerrarSesion,
        onReintentarFicha = { viewModel.actualizarFicha(forzar = true) },
        modifier = modifier,
    )
}

@Composable
private fun PerfilContent(
    estado: PerfilUiState,
    onIniciarSesion: () -> Unit,
    onCerrarSesion: () -> Unit,
    onReintentarFicha: () -> Unit,
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
            FichaPropia(estado, sesion, onReintentarFicha)
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

/**
 * RF03: la ficha de quien inició sesión, como "Bienvenido …" en el sitio. Se baja por código, así
 * que aparece aunque el jugador no esté entre los primeros 100 de su división.
 */
@Composable
private fun FichaPropia(estado: PerfilUiState, sesion: SesionJugador, onReintentar: () -> Unit) {
    val ficha = estado.ficha
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(modifier = Modifier.semantics(mergeDescendants = true) {}) {
                Text(
                    text = stringResource(R.string.ficha_tu_perfil),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = ficha?.nombre ?: sesion.nombre ?: sesion.codigo,
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.semantics { heading() },
                )
                Text(
                    text = stringResource(R.string.perfil_codigo, sesion.codigo),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            when {
                ficha != null -> DatosFicha(ficha)
                estado.actualizandoFicha -> Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Text(stringResource(R.string.perfil_bajando_ficha), style = MaterialTheme.typography.bodyMedium)
                }
                estado.falloFicha != null -> Column {
                    Text(
                        text = stringResource(estado.falloFicha.mensaje),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    TextButton(onClick = onReintentar, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text(stringResource(R.string.estado_reintentar))
                    }
                }
            }
        }
    }
}

@Composable
private fun DatosFicha(ficha: Jugador) {
    Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
        Estadistica(stringResource(R.string.ficha_division), ficha.division)
        ficha.rating?.let { Estadistica(stringResource(R.string.ficha_rating), it.toString()) }
    }
    ficha.club?.let {
        Text(
            text = stringResource(R.string.perfil_club, it),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
    ficha.actualizadoEn?.let { actualizado ->
        val patron = stringResource(R.string.formato_fecha_ultima_actualizacion)
        val fecha = remember(actualizado, patron) {
            DateTimeFormatter.ofPattern(patron).format(actualizado.atZone(ZoneId.systemDefault()))
        }
        Text(
            text = stringResource(R.string.jugadores_actualizado, fecha),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun Estadistica(etiqueta: String, valor: String) {
    Column(modifier = Modifier.semantics(mergeDescendants = true) {}) {
        Text(text = valor, style = MaterialTheme.typography.titleLarge)
        Text(
            text = etiqueta,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
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
internal val EstadoInscripcion.etiqueta: Int
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
                    sesion = SesionJugador("99999", "Jugador Demo"),
                    ficha = Jugador(
                        codigo = 99999,
                        nombre = "Jugador Demo",
                        division = "6ta",
                        puesto = null,
                        rating = 1300,
                        actualizadoEn = Instant.EPOCH,
                    ),
                    inscripciones = listOf(
                        InscripcionItem(1, "Torneo de prueba", "6ta", EstadoInscripcion.RESERVADA, Instant.EPOCH),
                        InscripcionItem(2, null, "7ma", EstadoInscripcion.LISTA_ESPERA, Instant.EPOCH),
                    ),
                ),
                onIniciarSesion = {},
                onCerrarSesion = {},
                onReintentarFicha = {},
            )
        }
    }
}
