package com.uade.tmt.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.uade.tmt.R
import com.uade.tmt.presentation.theme.TMTTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Franja que acompaña a los datos locales (RNF01): indica cuándo se actualizaron por última
 * vez y, si [isOffline] es true, avisa que se están mostrando datos guardados porque falló la red.
 *
 * @param lastUpdatedMillis momento de la última sincronización exitosa, en epoch millis.
 */
@Composable
fun LastUpdatedBanner(
    lastUpdatedMillis: Long,
    modifier: Modifier = Modifier,
    isOffline: Boolean = false,
) {
    val pattern = stringResource(R.string.formato_fecha_ultima_actualizacion)
    val formattedDate = remember(lastUpdatedMillis, pattern) {
        DateTimeFormatter.ofPattern(pattern, Locale.getDefault())
            .format(Instant.ofEpochMilli(lastUpdatedMillis).atZone(ZoneId.systemDefault()))
    }

    val containerColor = if (isOffline) {
        MaterialTheme.colorScheme.tertiaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = if (isOffline) {
        MaterialTheme.colorScheme.onTertiaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
        color = containerColor,
        contentColor = contentColor,
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = MinTouchTarget)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isOffline) {
                Icon(
                    painter = painterResource(R.drawable.ic_estado_sin_conexion),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
            }
            Column {
                if (isOffline) {
                    Text(
                        text = stringResource(R.string.estado_aviso_datos_guardados),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
                Text(
                    text = stringResource(R.string.estado_ultima_actualizacion, formattedDate),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

private const val PREVIEW_MILLIS = 1_763_900_000_000L

@PreviewLightDark
@Composable
private fun LastUpdatedBannerPreview() {
    TMTTheme(dynamicColor = false) {
        Surface {
            LastUpdatedBanner(lastUpdatedMillis = PREVIEW_MILLIS)
        }
    }
}

@PreviewLightDark
@Composable
private fun LastUpdatedBannerOfflinePreview() {
    TMTTheme(dynamicColor = false) {
        Surface {
            LastUpdatedBanner(lastUpdatedMillis = PREVIEW_MILLIS, isOffline = true)
        }
    }
}
