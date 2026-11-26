package com.uade.tmt.presentation.reserva

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.uade.tmt.R
import com.uade.tmt.domain.model.OpcionInscripcion
import com.uade.tmt.domain.model.ResultadoReserva
import com.uade.tmt.presentation.components.EmptyState
import com.uade.tmt.presentation.components.ErrorState
import com.uade.tmt.presentation.components.LoadingState
import com.uade.tmt.presentation.components.OfflineState
import com.uade.tmt.presentation.login.AvisoError
import com.uade.tmt.presentation.theme.TMTTheme

/**
 * RF02: reserva de lugar. En el camino corto son 3 toques desde que se abre (categoría,
 * condiciones, confirmar) y 2 si hay una sola categoría habilitada, que ya viene elegida (RNF08).
 */
@Composable
fun ReservaScreen(
    onVolver: () -> Unit,
    onIrALogin: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ReservaViewModel = viewModel(factory = ReservaViewModel.Factory),
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(estado.irALogin) {
        if (estado.irALogin) {
            viewModel.loginAbierto()
            onIrALogin()
        }
    }

    ReservaContent(
        estado = estado,
        acciones = AccionesReserva(
            onVolver = onVolver,
            onRecargar = viewModel::cargarOpciones,
            onElegir = viewModel::elegir,
            onCondiciones = viewModel::aceptarCondiciones,
            onConfirmar = viewModel::confirmar,
            onReintentar = viewModel::reintentar,
            onIniciarSesion = viewModel::iniciarSesion,
        ),
        modifier = modifier,
    )
}

private class AccionesReserva(
    val onVolver: () -> Unit,
    val onRecargar: () -> Unit,
    val onElegir: (String) -> Unit,
    val onCondiciones: (Boolean) -> Unit,
    val onConfirmar: () -> Unit,
    val onReintentar: () -> Unit,
    val onIniciarSesion: () -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReservaContent(
    estado: ReservaUiState,
    acciones: AccionesReserva,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(stringResource(R.string.reserva_titulo)) },
            navigationIcon = {
                IconButton(onClick = acciones.onVolver) {
                    Icon(painterResource(R.drawable.ic_volver), stringResource(R.string.reserva_volver))
                }
            },
            windowInsets = WindowInsets(0),
        )
        when (estado.carga) {
            CargaOpciones.CARGANDO -> LoadingState(message = stringResource(R.string.reserva_cargando))
            CargaOpciones.SIN_CONEXION -> OfflineState(onRetry = acciones.onRecargar)
            CargaOpciones.ERROR_SITIO -> ErrorState(
                onRetry = acciones.onRecargar,
                message = stringResource(R.string.reserva_error_opciones),
            )
            CargaOpciones.SESION_REQUERIDA -> EmptyState(
                message = stringResource(R.string.reserva_sesion_requerida),
                actionLabel = stringResource(R.string.reserva_iniciar_sesion),
                onAction = acciones.onIniciarSesion,
            )
            CargaOpciones.LISTA -> when {
                estado.terminada -> ResultadoFinal(estado, acciones.onVolver)
                estado.opciones.isEmpty() -> EmptyState(message = stringResource(R.string.reserva_sin_categorias))
                else -> Formulario(estado, acciones)
            }
        }
    }
}

@Composable
private fun Formulario(estado: ReservaUiState, acciones: AccionesReserva) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Titulo(stringResource(R.string.reserva_elegi_categoria))
        Column(modifier = Modifier.selectableGroup()) {
            estado.opciones.forEach { opcion ->
                FilaOpcion(
                    opcion = opcion,
                    elegida = opcion.categoria == estado.seleccionada,
                    habilitada = opcion.habilitada && !estado.enviando,
                    onElegir = { acciones.onElegir(opcion.categoria) },
                )
            }
        }

        Titulo(stringResource(R.string.reserva_condiciones_titulo))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .toggleable(
                    value = estado.condicionesAceptadas,
                    enabled = !estado.enviando,
                    role = Role.Checkbox,
                    onValueChange = acciones.onCondiciones,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // El Row ya es el control: el Checkbox no recibe toques propios.
            Checkbox(checked = estado.condicionesAceptadas, onCheckedChange = null, enabled = !estado.enviando)
            Text(
                text = stringResource(R.string.reserva_condiciones),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(start = 12.dp),
            )
        }

        AvisoResultado(estado, acciones)

        if (!estado.conectado) {
            Text(
                text = stringResource(R.string.reserva_sin_conexion_aviso),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        }

        Button(
            onClick = acciones.onConfirmar,
            enabled = estado.puedeConfirmar,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp),
        ) {
            if (estado.enviando) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .padding(end = 12.dp)
                        .size(20.dp),
                    strokeWidth = 2.dp,
                )
                Text(stringResource(R.string.reserva_enviando))
            } else {
                Text(stringResource(R.string.reserva_confirmar))
            }
        }
    }
}

@Composable
private fun FilaOpcion(
    opcion: OpcionInscripcion,
    elegida: Boolean,
    habilitada: Boolean,
    onElegir: () -> Unit,
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .selectable(selected = elegida, enabled = habilitada, role = Role.RadioButton, onClick = onElegir)
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(selected = elegida, onClick = null, enabled = habilitada)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp),
            ) {
                val color = if (opcion.habilitada) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
                Text(text = opcion.categoria, style = MaterialTheme.typography.bodyLarge, color = color)
                opcion.motivo?.takeIf { !opcion.habilitada }?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            opcion.horario?.let { horario ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(R.drawable.ic_horario),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .size(18.dp),
                    )
                    Text(
                        text = stringResource(R.string.reserva_horario, horario),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
        HorizontalDivider()
    }
}

/** Resultados que dejan el formulario abierto: rechazo, sin conexión, error, falta de sesión. */
@Composable
private fun AvisoResultado(estado: ReservaUiState, acciones: AccionesReserva) {
    when (val resultado = estado.resultado) {
        is ResultadoReserva.Rechazada -> AvisoError(texto = resultado.motivo, onReintentar = null)
        ResultadoReserva.SinConexion -> AvisoError(
            texto = stringResource(R.string.reserva_resultado_sin_conexion),
            onReintentar = acciones.onReintentar.takeIf { estado.ofreceReintentar },
        )
        ResultadoReserva.ErrorSitio -> AvisoError(
            texto = stringResource(R.string.reserva_resultado_error_sitio),
            onReintentar = acciones.onReintentar.takeIf { estado.ofreceReintentar },
        )
        else -> Unit
    }
    if (estado.pideLogin) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.reserva_sesion_requerida),
                style = MaterialTheme.typography.bodyMedium,
            )
            OutlinedButton(
                onClick = acciones.onIniciarSesion,
                modifier = Modifier.heightIn(min = 48.dp),
            ) { Text(stringResource(R.string.reserva_iniciar_sesion)) }
        }
    }
}

/** Reservada, lista de espera o incierta: no se puede volver a enviar desde acá. */
@Composable
private fun ResultadoFinal(estado: ReservaUiState, onVolver: () -> Unit) {
    val enlaces = LocalUriHandler.current
    val urlTorneo = stringResource(R.string.url_torneo_sitio, estado.torneoCodigo)
    val opcion = estado.opcionElegida
    val categoria = opcion?.categoria ?: estado.seleccionada.orEmpty()

    val (titulo, detalle) = when (estado.resultado) {
        ResultadoReserva.Reservada -> stringResource(R.string.reserva_resultado_reservada) to
            (opcion?.horario?.let { stringResource(R.string.reserva_resultado_reservada_detalle_horario, categoria, it) }
                ?: stringResource(R.string.reserva_resultado_reservada_detalle, categoria))
        ResultadoReserva.ListaEspera -> stringResource(R.string.reserva_resultado_espera) to
            stringResource(R.string.reserva_resultado_espera_detalle, categoria)
        else -> stringResource(R.string.reserva_resultado_incierta) to null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
            .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = titulo,
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )
        detalle?.let {
            Text(text = it, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
        }
        // Incierta: sin reintentar; solo revisar en el sitio.
        if (estado.resultado == ResultadoReserva.Incierta) {
            Button(
                onClick = { enlaces.openUri(urlTorneo) },
                modifier = Modifier.heightIn(min = 48.dp),
            ) { Text(stringResource(R.string.reserva_ver_en_sitio)) }
        }
        OutlinedButton(
            onClick = onVolver,
            modifier = Modifier.heightIn(min = 48.dp),
        ) { Text(stringResource(R.string.reserva_volver_torneo)) }
    }
}

@Composable
private fun Titulo(texto: String) {
    Text(
        text = texto,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.semantics { heading() },
    )
}

private val AccionesVacias = AccionesReserva({}, {}, {}, {}, {}, {}, {})

@PreviewLightDark
@Composable
private fun ReservaFormularioPreview() {
    TMTTheme {
        Surface {
            ReservaContent(
                estado = ReservaUiState(
                    torneoCodigo = 1,
                    carga = CargaOpciones.LISTA,
                    opciones = listOf(
                        OpcionInscripcion("5ta", "10:00", habilitada = false, motivo = "Tu rating no habilita esta categoría"),
                        OpcionInscripcion("6ta", "12:00"),
                        OpcionInscripcion("7ma", "14:00"),
                    ),
                    seleccionada = "6ta",
                    resultado = ResultadoReserva.ErrorSitio,
                ),
                acciones = AccionesVacias,
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun ReservaInciertaPreview() {
    TMTTheme {
        Surface {
            ReservaContent(
                estado = ReservaUiState(
                    torneoCodigo = 1,
                    carga = CargaOpciones.LISTA,
                    opciones = listOf(OpcionInscripcion("6ta", "12:00")),
                    seleccionada = "6ta",
                    resultado = ResultadoReserva.Incierta,
                ),
                acciones = AccionesVacias,
            )
        }
    }
}
