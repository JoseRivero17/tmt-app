package com.uade.tmt.presentation.ranking

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.uade.tmt.R
import com.uade.tmt.domain.model.Jugador
import com.uade.tmt.presentation.components.EmptyState
import com.uade.tmt.presentation.components.LastUpdatedBanner
import com.uade.tmt.presentation.components.LoadingState
import com.uade.tmt.presentation.theme.TMTTheme
import java.time.Instant

/** Ficha de un jugador (RF03): datos del ranking y botón para seguirlo. El perfil propio sale del login. */
@Composable
fun FichaJugadorScreen(
    onVolver: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FichaJugadorViewModel = viewModel(factory = FichaJugadorViewModel.Factory),
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    FichaJugadorContent(
        estado = estado,
        onVolver = onVolver,
        onSeguir = viewModel::alternarSeguir,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FichaJugadorContent(
    estado: FichaUiState,
    onVolver: () -> Unit,
    onSeguir: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(stringResource(R.string.ficha_titulo)) },
            navigationIcon = {
                IconButton(onClick = onVolver) {
                    Icon(painterResource(R.drawable.ic_volver), stringResource(R.string.ficha_volver))
                }
            },
            // El Scaffold principal ya reserva el espacio de la barra de estado.
            windowInsets = WindowInsets(0),
        )
        when (estado) {
            FichaUiState.Cargando -> LoadingState()
            FichaUiState.NoEncontrado -> EmptyState(message = stringResource(R.string.ficha_no_encontrado))
            is FichaUiState.Contenido -> {
                estado.jugador.actualizadoEn?.let {
                    LastUpdatedBanner(lastUpdatedMillis = it.toEpochMilli())
                }
                DatosJugador(estado, onSeguir)
            }
        }
    }
}

@Composable
private fun DatosJugador(
    estado: FichaUiState.Contenido,
    onSeguir: () -> Unit,
) {
    val jugador = estado.jugador
    val sinDato = stringResource(R.string.ficha_sin_dato)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (estado.esPropio) {
                Text(
                    text = stringResource(R.string.ficha_tu_perfil),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Text(
                text = jugador.nombre,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = stringResource(R.string.ficha_codigo, jugador.codigo),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Column {
            Dato(stringResource(R.string.ficha_division), jugador.division)
            Dato(
                stringResource(R.string.ficha_puesto),
                jugador.puesto?.let { stringResource(R.string.jugadores_puesto, it) } ?: sinDato,
            )
            Dato(stringResource(R.string.ficha_rating), jugador.rating?.toString() ?: sinDato)
            Dato(stringResource(R.string.ficha_club), jugador.club ?: sinDato)
        }

        // El perfil propio lo define la sesión: no se sigue ni se deja de seguir desde acá.
        if (estado.esPropio) {
            Text(
                text = stringResource(R.string.ficha_perfil_por_sesion),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else if (estado.seguido) {
            OutlinedButton(
                onClick = onSeguir,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
            ) { Text(stringResource(R.string.ficha_dejar_de_seguir)) }
        } else {
            Button(
                onClick = onSeguir,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
            ) { Text(stringResource(R.string.ficha_seguir)) }
        }
    }
}

@Composable
private fun Dato(etiqueta: String, valor: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {}
            .padding(vertical = 12.dp),
    ) {
        Text(
            text = etiqueta,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(text = valor, style = MaterialTheme.typography.titleMedium)
    }
    HorizontalDivider()
}

@PreviewLightDark
@Composable
private fun FichaPreview() {
    TMTTheme {
        Surface {
            FichaJugadorContent(
                estado = FichaUiState.Contenido(
                    jugador = Jugador(
                        codigo = 1234,
                        nombre = "PEREZ, JUAN DE MUESTRA",
                        division = "5ta",
                        puesto = 3,
                        rating = 1520,
                        club = "CMU",
                        actualizadoEn = Instant.ofEpochMilli(1_763_900_000_000L),
                    ),
                    seguido = true,
                    esPropio = true,
                ),
                onVolver = {},
                onSeguir = {},
            )
        }
    }
}