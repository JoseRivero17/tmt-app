package com.uade.tmt.presentation.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.uade.tmt.R
import com.uade.tmt.domain.model.MotivoFallo
import com.uade.tmt.presentation.mensaje
import com.uade.tmt.presentation.theme.TMTTheme

/** Falló la actualización pero hay datos guardados: avisa sin taparlos y permite reintentar (RNF02). */
@Composable
fun FalloBanner(
    motivo: MotivoFallo,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .semantics { liveRegion = LiveRegionMode.Polite },
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = MinTouchTarget)
                .padding(start = 16.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(motivo.mensaje),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onRetry) {
                Text(
                    text = stringResource(R.string.estado_reintentar),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun FalloBannerPreview() {
    TMTTheme(dynamicColor = false) {
        FalloBanner(motivo = MotivoFallo.TIMEOUT, onRetry = {})
    }
}
