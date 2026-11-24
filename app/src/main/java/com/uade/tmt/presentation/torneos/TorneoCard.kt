package com.uade.tmt.presentation.torneos

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.uade.tmt.R
import com.uade.tmt.domain.model.Torneo
import com.uade.tmt.presentation.theme.TMTTheme

/** Tarjeta del calendario. La división elegida en el filtro se destaca. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun TorneoCard(
    torneo: Torneo,
    divisionElegida: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedCard(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                EstadoTorneo(torneo)
                Spacer(modifier = Modifier.weight(1f))
                Icon(painterResource(R.drawable.ic_avanzar), contentDescription = null)
            }
            Text(text = torneo.nombre, style = MaterialTheme.typography.titleMedium)
            Renglon(R.drawable.ic_clubes) {
                Text(text = torneo.localidad, style = MaterialTheme.typography.bodyMedium)
            }
            if (torneo.categorias.isNotEmpty()) {
                Renglon(R.drawable.ic_horario) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        torneo.categorias.forEach { categoria ->
                            val elegida = categoria.division == divisionElegida
                            Text(
                                text = listOfNotNull(categoria.division, categoria.hora).joinToString(" "),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (elegida) FontWeight.Bold else null,
                                // Naranja: el color de lo propio del jugador (4.8).
                                color = if (elegida) MaterialTheme.colorScheme.tertiary else Color.Unspecified,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun Renglon(
    @DrawableRes icono: Int,
    modifier: Modifier = Modifier,
    contenido: @Composable () -> Unit,
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(
            painter = painterResource(icono),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
        contenido()
    }
}

/** El estado se comunica con ícono y texto, no solo con color (RNF09). */
@Composable
internal fun EstadoTorneo(torneo: Torneo, modifier: Modifier = Modifier) {
    val m3 = MaterialTheme.colorScheme
    val tmt = TMTTheme.colores
    when {
        torneo.cancelado -> Insignia(
            R.string.torneos_estado_cancelado, R.drawable.ic_cancelado,
            m3.errorContainer, m3.onErrorContainer, modifier,
        )

        torneo.inscripcionOnline -> Insignia(
            R.string.torneos_estado_inscripcion, R.drawable.ic_abierta,
            tmt.exitoContenedor, tmt.sobreExitoContenedor, modifier,
        )

        else -> Insignia(
            R.string.torneos_estado_sin_inscripcion, R.drawable.ic_cancelado,
            m3.surfaceContainerHighest, m3.onSurface, modifier,
        )
    }
}

@Composable
private fun Insignia(
    texto: Int,
    @DrawableRes icono: Int,
    fondo: Color,
    contenido: Color,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier, color = fondo, contentColor = contenido, shape = CircleShape) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(painterResource(icono), contentDescription = null, modifier = Modifier.size(16.dp))
            Text(text = stringResource(texto), style = MaterialTheme.typography.labelMedium)
        }
    }
}
