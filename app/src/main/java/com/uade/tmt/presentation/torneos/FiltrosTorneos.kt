package com.uade.tmt.presentation.torneos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.uade.tmt.R
import com.uade.tmt.domain.model.FiltrosTorneo
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/** Filtros de RF01: división, localidad y rango de fechas. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun FiltrosTorneos(
    filtros: FiltrosTorneo,
    opciones: OpcionesFiltro,
    onFiltrar: (FiltrosTorneo) -> Unit,
    onLimpiar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Bajan de renglón si no entran: nada queda oculto a la derecha ni con letra grande (RNF09).
    FlowRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        ChipConMenu(
            etiqueta = stringResource(R.string.torneos_filtro_division),
            elegido = filtros.division,
            opciones = opciones.divisiones,
            onElegir = { onFiltrar(filtros.copy(division = it)) },
        )
        ChipConMenu(
            etiqueta = stringResource(R.string.torneos_filtro_localidad),
            elegido = filtros.localidad,
            opciones = opciones.localidades,
            onElegir = { onFiltrar(filtros.copy(localidad = it)) },
        )
        ChipFechas(
            desde = filtros.desde,
            hasta = filtros.hasta,
            onElegir = { desde, hasta -> onFiltrar(filtros.copy(desde = desde, hasta = hasta)) },
        )
        if (filtros.hayAlguno) {
            TextButton(onClick = onLimpiar) { Text(stringResource(R.string.estado_limpiar_filtros)) }
        }
    }
}

@Composable
private fun ChipConMenu(
    etiqueta: String,
    elegido: String?,
    opciones: List<String>,
    onElegir: (String?) -> Unit,
) {
    var abierto by rememberSaveable { mutableStateOf(false) }
    Box {
        FilterChip(
            selected = elegido != null,
            onClick = { abierto = true },
            label = { Text(if (elegido == null) etiqueta else "$etiqueta: $elegido") },
            trailingIcon = { Icon(painterResource(R.drawable.ic_desplegar), contentDescription = null) },
        )
        DropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }) {
            (listOf<String?>(null) + opciones).forEach { opcion ->
                DropdownMenuItem(
                    text = { Text(opcion ?: stringResource(R.string.torneos_filtro_todas)) },
                    onClick = {
                        abierto = false
                        onElegir(opcion)
                    },
                )
            }
        }
    }
}

@Composable
private fun ChipFechas(
    desde: LocalDate?,
    hasta: LocalDate?,
    onElegir: (LocalDate?, LocalDate?) -> Unit,
) {
    var abierto by rememberSaveable { mutableStateOf(false) }
    FilterChip(
        selected = desde != null || hasta != null,
        onClick = { abierto = true },
        label = {
            Text(
                if (desde != null && hasta != null) {
                    stringResource(R.string.torneos_filtro_rango, desde.corta(), hasta.corta())
                } else {
                    stringResource(R.string.torneos_filtro_fechas)
                },
            )
        },
        trailingIcon = { Icon(painterResource(R.drawable.ic_desplegar), contentDescription = null) },
    )
    if (abierto) {
        DialogoFechas(
            desde = desde,
            hasta = hasta,
            onCerrar = { abierto = false },
            onElegir = { d, h ->
                abierto = false
                onElegir(d, h)
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DialogoFechas(
    desde: LocalDate?,
    hasta: LocalDate?,
    onCerrar: () -> Unit,
    onElegir: (LocalDate?, LocalDate?) -> Unit,
) {
    val estado = rememberDateRangePickerState(
        initialSelectedStartDateMillis = desde?.aMilis(),
        initialSelectedEndDateMillis = hasta?.aMilis(),
    )
    DatePickerDialog(
        onDismissRequest = onCerrar,
        confirmButton = {
            TextButton(
                // Elegir un solo día equivale a filtrar ese día.
                onClick = {
                    val inicio = estado.selectedStartDateMillis?.aFecha()
                    onElegir(inicio, estado.selectedEndDateMillis?.aFecha() ?: inicio)
                },
                enabled = estado.selectedStartDateMillis != null,
            ) { Text(stringResource(R.string.torneos_aplicar)) }
        },
        dismissButton = {
            TextButton(onClick = onCerrar) { Text(stringResource(R.string.torneos_cancelar)) }
        },
    ) {
        DateRangePicker(
            state = estado,
            showModeToggle = false,
            title = {
                Text(
                    text = stringResource(R.string.torneos_filtro_fechas),
                    modifier = Modifier.padding(start = 24.dp, top = 16.dp),
                )
            },
            modifier = Modifier.weight(1f),
        )
    }
}

// El selector de fechas trabaja en UTC.
private fun LocalDate.aMilis(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

private fun Long.aFecha(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()
