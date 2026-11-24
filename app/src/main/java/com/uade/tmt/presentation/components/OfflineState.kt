package com.uade.tmt.presentation.components

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.uade.tmt.R
import com.uade.tmt.presentation.theme.TMTTheme

/**
 * Pantalla completa "Sin conexión" (RNF01, RNF02). Solo para cuando no hay red Y tampoco hay
 * datos guardados. Si hay datos locales, se muestran con [LastUpdatedBanner] en su lugar.
 */
@Composable
fun OfflineState(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    StateMessageLayout(
        iconRes = R.drawable.ic_estado_sin_conexion,
        title = stringResource(R.string.estado_sin_conexion_titulo),
        message = stringResource(R.string.estado_sin_conexion_mensaje),
        modifier = modifier,
        actionLabel = stringResource(R.string.estado_reintentar),
        onAction = onRetry,
    )
}

@PreviewLightDark
@Composable
private fun OfflineStatePreview() {
    TMTTheme {
        Surface {
            OfflineState(onRetry = {})
        }
    }
}
