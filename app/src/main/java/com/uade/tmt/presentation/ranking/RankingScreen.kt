package com.uade.tmt.presentation.ranking

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.uade.tmt.R
import com.uade.tmt.domain.model.DIVISIONES
import com.uade.tmt.domain.model.Jugador
import com.uade.tmt.domain.model.Seguido
import com.uade.tmt.presentation.components.EmptyState
import com.uade.tmt.presentation.components.ErrorState
import com.uade.tmt.presentation.components.FalloBanner
import com.uade.tmt.presentation.components.LastUpdatedBanner
import com.uade.tmt.presentation.components.LoadingState
import com.uade.tmt.presentation.components.OfflineState
import com.uade.tmt.presentation.components.StateMessageLayout
import com.uade.tmt.presentation.mensaje
import com.uade.tmt.presentation.theme.TMTTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Pestaña Jugadores (RF03): ranking por división y búsqueda. */
@Composable
fun RankingScreen(
    modifier: Modifier = Modifier,
    onJugadorClick: (codigo: Int) -> Unit = {},
    viewModel: RankingViewModel = viewModel(factory = RankingViewModel.Factory),
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    val conectado by viewModel.conectado.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_START) { viewModel.actualizar(forzar = false) }
    LaunchedEffect(conectado) { if (conectado) viewModel.actualizar(forzar = false) }

    RankingContent(
        estado = estado,
        onElegirPestana = viewModel::elegirPestana,
        onBuscar = viewModel::buscar,
        onElegirDivision = viewModel::elegirDivision,
        onActualizar = {
            if (estado.pestana == PestanaJugadores.SEGUIDOS) {
                viewModel.actualizarSeguidos()
            } else {
                viewModel.actualizar(forzar = true)
            }
        },
        onJugadorClick = onJugadorClick,
        modifier = modifier,
    )
}

/** Pestañas Ranking / Seguidos. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RankingContent(
    estado: RankingUiState,
    onElegirPestana: (PestanaJugadores) -> Unit,
    onBuscar: (String) -> Unit,
    onElegirDivision: (String) -> Unit,
    onActualizar: () -> Unit,
    onJugadorClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        PrimaryTabRow(selectedTabIndex = estado.pestana.ordinal) {
            Tab(
                selected = estado.pestana == PestanaJugadores.RANKING,
                onClick = { onElegirPestana(PestanaJugadores.RANKING) },
                text = { Text(stringResource(R.string.jugadores_tab_ranking)) },
            )
            Tab(
                selected = estado.pestana == PestanaJugadores.SEGUIDOS,
                onClick = { onElegirPestana(PestanaJugadores.SEGUIDOS) },
                text = { Text(stringResource(R.string.jugadores_tab_seguidos, estado.seguidos.size)) },
            )
        }
        when (estado.pestana) {
            PestanaJugadores.RANKING -> PestanaRanking(
                estado = estado,
                onBuscar = onBuscar,
                onElegirDivision = onElegirDivision,
                onActualizar = onActualizar,
                onJugadorClick = onJugadorClick,
            )
            PestanaJugadores.SEGUIDOS -> PestanaSeguidos(
                estado = estado,
                onActualizar = onActualizar,
                onJugadorClick = onJugadorClick,
            )
        }
    }
}

/** Seguidos y perfil propio: salen de lo guardado, así que se ven sin conexión (RF03). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PestanaSeguidos(
    estado: RankingUiState,
    onActualizar: () -> Unit,
    onJugadorClick: (Int) -> Unit,
) {
    val seguidos = estado.seguidos
    if (seguidos.isEmpty()) {
        EmptyState(message = stringResource(R.string.jugadores_sin_seguidos))
        return
    }
    Column(modifier = Modifier.fillMaxSize()) {
        // La fecha más reciente entre los seguidos, con el aviso si no hay conexión.
        seguidos.mapNotNull { it.jugador.actualizadoEn }.maxOrNull()?.let {
            LastUpdatedBanner(lastUpdatedMillis = it.toEpochMilli(), isOffline = estado.sinConexion)
        }
        val fallo = estado.fallo
        if (fallo != null) {
            FalloBanner(motivo = fallo, onRetry = onActualizar)
        }
        PullToRefreshBox(
            isRefreshing = estado.actualizando,
            onRefresh = onActualizar,
            modifier = Modifier.fillMaxSize(),
        ) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(seguidos, key = { it.jugador.codigo }) { seguido ->
                    FilaJugador(
                        jugador = seguido.jugador,
                        onClick = { onJugadorClick(seguido.jugador.codigo) },
                        etiqueta = if (seguido.esPropio) stringResource(R.string.ficha_tu_perfil) else null,
                        mostrarFecha = true,
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PestanaRanking(
    estado: RankingUiState,
    onBuscar: (String) -> Unit,
    onElegirDivision: (String) -> Unit,
    onActualizar: () -> Unit,
    onJugadorClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val texto = estado.busqueda.trim()
    // Escribió algo, pero todavía no alcanza para buscar.
    val escribiendo = texto.isNotEmpty() && !estado.buscando

    Column(modifier = modifier.fillMaxSize()) {
        OutlinedTextField(
            value = estado.busqueda,
            onValueChange = onBuscar,
            label = { Text(stringResource(R.string.jugadores_buscar)) },
            leadingIcon = { Icon(painterResource(R.drawable.ic_buscar), contentDescription = null) },
            trailingIcon = {
                if (estado.busqueda.isNotEmpty()) {
                    IconButton(onClick = { onBuscar("") }) {
                        Icon(
                            painterResource(R.drawable.ic_cerrar),
                            contentDescription = stringResource(R.string.jugadores_borrar_busqueda),
                        )
                    }
                }
            },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        )

        // Mientras se escribe o se busca, las divisiones no aplican: se ocultan.
        if (texto.isEmpty()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(DIVISIONES) { division ->
                    FilterChip(
                        selected = division == estado.division,
                        onClick = { onElegirDivision(division) },
                        label = { Text(division) },
                    )
                }
            }
            estado.actualizadoEn?.let {
                LastUpdatedBanner(lastUpdatedMillis = it.toEpochMilli(), isOffline = estado.sinConexion)
            }
        }
        val fallo = estado.fallo
        if (fallo != null && estado.actualizadoEn != null && texto.isEmpty()) {
            FalloBanner(motivo = fallo, onRetry = onActualizar)
        }

        // Todavía no hay nada guardado de esta división.
        val sinDatos = estado.jugadores.isEmpty() && estado.actualizadoEn == null && texto.isEmpty()
        when {
            escribiendo -> StateMessageLayout(
                iconRes = R.drawable.ic_buscar,
                message = stringResource(R.string.jugadores_buscar_ayuda),
            )
            sinDatos && estado.sinConexion -> OfflineState(onRetry = onActualizar)
            sinDatos && fallo != null -> ErrorState(onRetry = onActualizar, message = stringResource(fallo.mensaje))
            sinDatos -> LoadingState()
            estado.buscando && estado.jugadores.isEmpty() -> EmptyState(
                message = stringResource(R.string.jugadores_sin_resultados, texto),
            )
            else -> PullToRefreshBox(
                isRefreshing = estado.actualizando,
                onRefresh = onActualizar,
                modifier = Modifier.fillMaxSize(),
            ) {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    if (estado.buscando) {
                        item(key = "encabezado") { EncabezadoResultados(estado.jugadores.size, texto) }
                    }
                    if (estado.jugadores.isEmpty()) {
                        item {
                            EmptyState(
                                message = stringResource(R.string.jugadores_sin_datos),
                                modifier = Modifier.fillParentMaxSize(),
                            )
                        }
                    }
                    items(estado.jugadores, key = { it.codigo }) { jugador ->
                        FilaJugador(jugador = jugador, onClick = { onJugadorClick(jugador.codigo) })
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

/** Deja claro que la lista es el resultado de una búsqueda y no el ranking. */
@Composable
private fun EncabezadoResultados(cantidad: Int, texto: String) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        modifier = Modifier
            .fillMaxWidth()
            .semantics { liveRegion = LiveRegionMode.Polite },
    ) {
        Text(
            text = pluralStringResource(R.plurals.jugadores_resultados, cantidad, cantidad, texto),
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun FilaJugador(
    jugador: Jugador,
    onClick: () -> Unit,
    etiqueta: String? = null,
    mostrarFecha: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .heightIn(min = 56.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = jugador.puesto?.let { stringResource(R.string.jugadores_puesto, it) } ?: "–",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.widthIn(min = 40.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            if (etiqueta != null) {
                Text(
                    text = etiqueta,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Text(text = jugador.nombre, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = listOfNotNull(jugador.division, jugador.club).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            val actualizado = jugador.actualizadoEn
            if (mostrarFecha && actualizado != null) {
                val patron = stringResource(R.string.jugadores_formato_fecha)
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
        jugador.rating?.let {
            Text(
                text = stringResource(R.string.jugadores_rating, it),
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

private val JugadoresDePrueba = listOf(
    Jugador(1, "PEREZ, JUAN DE MUESTRA", "5ta", 1, 1520, club = "CMU"),
    Jugador(2, "GOMEZ, ANA INVENTADA", "5ta", 2, 1498, club = "OTR"),
)

@PreviewLightDark
@Composable
private fun RankingPreview() {
    TMTTheme {
        Surface {
            RankingContent(
                estado = RankingUiState(
                    division = "5ta",
                    jugadores = JugadoresDePrueba,
                    actualizadoEn = Instant.ofEpochMilli(1_763_900_000_000L),
                ),
                onElegirPestana = {},
                onBuscar = {},
                onElegirDivision = {},
                onActualizar = {},
                onJugadorClick = {},
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun BusquedaPreview() {
    TMTTheme {
        Surface {
            RankingContent(
                estado = RankingUiState(busqueda = "mue", jugadores = JugadoresDePrueba.take(1)),
                onElegirPestana = {},
                onBuscar = {},
                onElegirDivision = {},
                onActualizar = {},
                onJugadorClick = {},
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun SeguidosPreview() {
    TMTTheme {
        Surface {
            RankingContent(
                estado = RankingUiState(
                    pestana = PestanaJugadores.SEGUIDOS,
                    seguidos = JugadoresDePrueba.mapIndexed { i, jugador ->
                        Seguido(jugador.copy(actualizadoEn = Instant.ofEpochMilli(1_763_900_000_000L)), esPropio = i == 0)
                    },
                ),
                onElegirPestana = {},
                onBuscar = {},
                onElegirDivision = {},
                onActualizar = {},
                onJugadorClick = {},
            )
        }
    }
}