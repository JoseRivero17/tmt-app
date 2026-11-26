package com.uade.tmt.presentation.clubes

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.uade.tmt.R
import com.uade.tmt.domain.model.Club
import com.uade.tmt.domain.model.ClubCercano
import com.uade.tmt.domain.model.MotivoFallo
import com.uade.tmt.presentation.components.EmptyState
import com.uade.tmt.presentation.components.ErrorState
import com.uade.tmt.presentation.components.FalloBanner
import com.uade.tmt.presentation.components.LastUpdatedBanner
import com.uade.tmt.presentation.components.LoadingState
import com.uade.tmt.presentation.components.OfflineState
import com.uade.tmt.presentation.mensaje
import com.uade.tmt.presentation.theme.TMTTheme
import java.time.Instant

/**
 * Clubes cercanos (RF04). La ubicación se pide recién acá, con una explicación previa (G1) y
 * precisión aproximada (RNF07). Si se rechaza, la función sigue con la búsqueda por localidad (G4).
 */
@Composable
fun ClubesScreen(
    onClubClick: (codigo: Int, distancia: String?) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ClubesViewModel = viewModel(factory = ClubesViewModel.Factory),
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    val conectado by viewModel.conectado.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // G2: el diálogo es del sistema; la app solo se entera de la respuesta.
    val pedirPermiso = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { concedido ->
        if (concedido) viewModel.permisoConcedido() else viewModel.permisoRechazado()
    }
    val usarUbicacion = {
        if (tienePermisoUbicacion(context)) {
            viewModel.permisoConcedido()
        } else {
            pedirPermiso.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
    }

    // Al entrar y al volver de Ajustes: el permiso pudo cambiar afuera de la app.
    LifecycleEventEffect(Lifecycle.Event.ON_START) {
        viewModel.actualizar(forzar = false)
        viewModel.revisarPermiso(tienePermisoUbicacion(context))
    }
    LaunchedEffect(conectado) { if (conectado) viewModel.actualizar(forzar = false) }

    ClubesContent(
        estado = estado,
        onUsarUbicacion = usarUbicacion,
        onContinuarSinUbicacion = viewModel::continuarSinUbicacion,
        onReintentarUbicacion = viewModel::leerUbicacion,
        onAbrirAjustes = { abrirAjustes(context) },
        onBuscar = viewModel::buscar,
        onActualizar = { viewModel.actualizar(forzar = true) },
        onClubClick = onClubClick,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClubesContent(
    estado: ClubesUiState,
    onUsarUbicacion: () -> Unit,
    onContinuarSinUbicacion: () -> Unit,
    onReintentarUbicacion: () -> Unit,
    onAbrirAjustes: () -> Unit,
    onBuscar: (String) -> Unit,
    onActualizar: () -> Unit,
    onClubClick: (Int, String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(stringResource(R.string.clubes_titulo)) },
            // El Scaffold principal ya reserva el espacio de la barra de estado.
            windowInsets = WindowInsets(0),
        )
        when (estado) {
            ClubesUiState.PedirPermiso -> ExplicacionUbicacion(onUsarUbicacion, onContinuarSinUbicacion)

            ClubesUiState.Cargando -> LoadingState(message = stringResource(R.string.estado_cargando))

            is ClubesUiState.Error -> if (estado.motivo == MotivoFallo.SIN_CONEXION) {
                OfflineState(onRetry = onActualizar)
            } else {
                ErrorState(onRetry = onActualizar, message = stringResource(estado.motivo.mensaje))
            }

            is ClubesUiState.Contenido -> ListaClubes(
                estado = estado,
                onUsarUbicacion = onUsarUbicacion,
                onReintentarUbicacion = onReintentarUbicacion,
                onAbrirAjustes = onAbrirAjustes,
                onBuscar = onBuscar,
                onActualizar = onActualizar,
                onClubClick = onClubClick,
            )
        }
    }
}

/** G1: para qué se usa la ubicación, antes del diálogo del sistema. También permite seguir sin ella. */
@Composable
private fun ExplicacionUbicacion(onUsarUbicacion: () -> Unit, onContinuarSinUbicacion: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_clubes),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(48.dp),
            )
        }
        Text(
            text = stringResource(R.string.clubes_permiso_titulo),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = stringResource(R.string.clubes_permiso_explicacion),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Button(
            onClick = onUsarUbicacion,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp),
        ) {
            Icon(painterResource(R.drawable.ic_cerca), contentDescription = null, modifier = Modifier.size(18.dp))
            Text(stringResource(R.string.clubes_usar_ubicacion), modifier = Modifier.padding(start = 8.dp))
        }
        OutlinedButton(
            onClick = onContinuarSinUbicacion,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp),
        ) {
            Icon(painterResource(R.drawable.ic_buscar), contentDescription = null, modifier = Modifier.size(18.dp))
            Text(stringResource(R.string.clubes_buscar_por_localidad), modifier = Modifier.padding(start = 8.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ListaClubes(
    estado: ClubesUiState.Contenido,
    onUsarUbicacion: () -> Unit,
    onReintentarUbicacion: () -> Unit,
    onAbrirAjustes: () -> Unit,
    onBuscar: (String) -> Unit,
    onActualizar: () -> Unit,
    onClubClick: (Int, String?) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        if (estado.acceso == AccesoUbicacion.RECHAZADO) {
            AvisoSinPermiso(onAbrirAjustes)
        }
        Buscador(texto = estado.busqueda, onBuscar = onBuscar)
        EstadoUbicacion(estado, onUsarUbicacion, onReintentarUbicacion)
        LastUpdatedBanner(
            lastUpdatedMillis = estado.actualizadoEn.toEpochMilli(),
            isOffline = estado.sinConexion,
        )
        if (estado.fallo != null) {
            FalloBanner(motivo = estado.fallo, onRetry = onActualizar)
        }
        if (estado.busqueda.isNotBlank() && estado.clubes.isNotEmpty()) {
            Text(
                text = pluralStringResource(
                    R.plurals.clubes_resultados,
                    estado.clubes.size,
                    estado.clubes.size,
                    estado.busqueda.trim(),
                ),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
        PullToRefreshBox(
            isRefreshing = estado.actualizando,
            onRefresh = onActualizar,
            modifier = Modifier.fillMaxSize(),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp),
            ) {
                if (estado.clubes.isEmpty()) {
                    item {
                        if (estado.busqueda.isNotBlank()) {
                            EmptyState(
                                message = stringResource(R.string.clubes_sin_coincidencias),
                                actionLabel = stringResource(R.string.clubes_limpiar_busqueda),
                                onAction = { onBuscar("") },
                                modifier = Modifier.fillParentMaxSize(),
                            )
                        } else {
                            EmptyState(
                                message = stringResource(R.string.clubes_sin_clubes),
                                modifier = Modifier.fillParentMaxSize(),
                            )
                        }
                    }
                }
                items(estado.clubes, key = { it.club.codigo }) { cercano ->
                    val distancia = cercano.distanciaKm?.let(::formatoDistancia)
                    FilaClub(
                        club = cercano.club,
                        distancia = distancia,
                        onClick = { onClubClick(cercano.club.codigo, distancia) },
                    )
                }
            }
        }
    }
}

/** G4: el permiso se rechazó; la función sigue y se indica cómo habilitarlo (criterio 2). */
@Composable
private fun AvisoSinPermiso(onAbrirAjustes: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = MaterialTheme.shapes.medium,
        color = TMTTheme.colores.avisoContenedor,
        contentColor = TMTTheme.colores.sobreAvisoContenedor,
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(painterResource(R.drawable.ic_aviso), contentDescription = null, modifier = Modifier.size(20.dp))
            Text(
                text = stringResource(R.string.clubes_permiso_rechazado),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onAbrirAjustes) {
                Text(
                    text = stringResource(R.string.clubes_ajustes),
                    color = TMTTheme.colores.sobreAvisoContenedor,
                )
            }
        }
    }
}

@Composable
private fun Buscador(texto: String, onBuscar: (String) -> Unit) {
    OutlinedTextField(
        value = texto,
        onValueChange = onBuscar,
        placeholder = { Text(stringResource(R.string.clubes_buscar_hint)) },
        leadingIcon = { Icon(painterResource(R.drawable.ic_buscar), contentDescription = null) },
        trailingIcon = {
            if (texto.isNotEmpty()) {
                IconButton(onClick = { onBuscar("") }) {
                    Icon(
                        painterResource(R.drawable.ic_cerrar),
                        contentDescription = stringResource(R.string.clubes_limpiar_busqueda),
                    )
                }
            }
        },
        singleLine = true,
        shape = MaterialTheme.shapes.extraLarge,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
    )
}

/** Chip "Cerca de vos" y qué está pasando con la ubicación (visibilidad del estado del sistema). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EstadoUbicacion(
    estado: ClubesUiState.Contenido,
    onUsarUbicacion: () -> Unit,
    onReintentarUbicacion: () -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        FilterChip(
            selected = estado.ordenadosPorDistancia,
            onClick = onUsarUbicacion,
            label = { Text(stringResource(R.string.clubes_cerca_de_vos)) },
            leadingIcon = {
                Icon(
                    painter = painterResource(if (estado.ordenadosPorDistancia) R.drawable.ic_elegido else R.drawable.ic_cerca),
                    contentDescription = null,
                    modifier = Modifier.size(FilterChipDefaults.IconSize),
                )
            },
            modifier = Modifier.heightIn(min = 48.dp),
        )
        val mensaje = when {
            estado.ubicando -> R.string.clubes_buscando_ubicacion
            estado.ordenadosPorDistancia -> R.string.clubes_ordenados_por_distancia
            estado.sinUbicacion -> R.string.clubes_sin_ubicacion
            else -> null
        }
        if (mensaje != null) {
            Row(
                modifier = Modifier.semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(mensaje),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                if (estado.sinUbicacion) {
                    TextButton(onClick = onReintentarUbicacion) {
                        Text(stringResource(R.string.estado_reintentar))
                    }
                }
            }
        }
    }
}

@Composable
private fun FilaClub(club: Club, distancia: String?, onClick: () -> Unit) {
    val distanciaAccesible = distancia?.let { stringResource(R.string.clubes_distancia_accesible, it) }
    val localidad = club.localidad
    ListItem(
        headlineContent = { Text(club.nombre, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = if (localidad != null) {
            { Text(localidad, maxLines = 1, overflow = TextOverflow.Ellipsis) }
        } else {
            null
        },
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_clubes),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(20.dp),
                )
            }
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (distancia != null) {
                    Text(
                        text = distancia,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.semantics { contentDescription = distanciaAccesible.orEmpty() },
                    )
                }
                Icon(painterResource(R.drawable.ic_avanzar), contentDescription = null)
            }
        },
        modifier = Modifier
            .clickable(onClick = onClick)
            .semantics(mergeDescendants = true) {},
    )
}

private fun tienePermisoUbicacion(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED

/** G4: "Ajustes" abre la pantalla de la app en los ajustes del sistema, donde se habilita el permiso. */
private fun abrirAjustes(context: Context) {
    val intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", context.packageName, null),
    )
    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        context.startActivity(Intent(Settings.ACTION_SETTINGS))
    }
}

@PreviewLightDark
@Composable
private fun ClubesPreview() {
    TMTTheme {
        Surface {
            ClubesContent(
                estado = ClubesUiState.Contenido(
                    clubes = listOf(
                        ClubCercano(Club(1, "Club Social Villa Crespo", localidad = "Villa Crespo"), 1.2),
                        ClubCercano(Club(2, "Ping Pong Almagro", localidad = "Almagro"), 2.4),
                        ClubCercano(Club(3, "Club sin ubicación", localidad = "La Plata"), null),
                    ),
                    busqueda = "",
                    acceso = AccesoUbicacion.CONCEDIDO,
                    ubicando = false,
                    sinUbicacion = false,
                    actualizadoEn = Instant.EPOCH,
                    actualizando = false,
                    fallo = null,
                    sinConexion = false,
                ),
                onUsarUbicacion = {},
                onContinuarSinUbicacion = {},
                onReintentarUbicacion = {},
                onAbrirAjustes = {},
                onBuscar = {},
                onActualizar = {},
                onClubClick = { _, _ -> },
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun ClubesPermisoPreview() {
    TMTTheme {
        Surface {
            ClubesContent(
                estado = ClubesUiState.PedirPermiso,
                onUsarUbicacion = {},
                onContinuarSinUbicacion = {},
                onReintentarUbicacion = {},
                onAbrirAjustes = {},
                onBuscar = {},
                onActualizar = {},
                onClubClick = { _, _ -> },
            )
        }
    }
}
