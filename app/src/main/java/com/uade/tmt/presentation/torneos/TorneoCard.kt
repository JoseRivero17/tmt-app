package com.uade.tmt.presentation.torneos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.uade.tmt.R
import com.uade.tmt.domain.model.Torneo

/** Tarjeta del calendario. La división elegida en el filtro se destaca. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun TorneoCard(
    torneo: Torneo,
    divisionElegida: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedCard(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            EstadoTorneo(torneo)
            Text(text = torneo.nombre, style = MaterialTheme.typography.titleMedium)
            Text(
                text = torneo.localidad,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                torneo.categorias.forEach { categoria ->
                    val elegida = categoria.division == divisionElegida
                    Text(
                        text = listOfNotNull(categoria.division, categoria.hora).joinToString(" "),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (elegida) FontWeight.Bold else null,
                        color = if (elegida) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

/** El estado se comunica con texto, no solo con color (RNF09). */
@Composable
internal fun EstadoTorneo(torneo: Torneo, modifier: Modifier = Modifier) {
    val colores = MaterialTheme.colorScheme
    val (texto, fondo, contenido) = when {
        torneo.cancelado ->
            Triple(R.string.torneos_estado_cancelado, colores.errorContainer, colores.onErrorContainer)

        torneo.inscripcionOnline ->
            Triple(R.string.torneos_estado_inscripcion, colores.primaryContainer, colores.onPrimaryContainer)

        else ->
            Triple(R.string.torneos_estado_sin_inscripcion, colores.surfaceVariant, colores.onSurfaceVariant)
    }
    Surface(modifier = modifier, color = fondo, contentColor = contenido, shape = MaterialTheme.shapes.small) {
        Text(
            text = stringResource(texto),
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}
