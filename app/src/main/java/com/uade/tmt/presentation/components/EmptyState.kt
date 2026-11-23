package com.uade.tmt.presentation.components

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.uade.tmt.R
import com.uade.tmt.presentation.theme.TMTTheme

/**
 * Estado vacío: la carga salió bien pero no hay elementos (por ejemplo, ningún torneo
 * coincide con los filtros). La acción es opcional; solo se muestra si llegan label y onClick.
 */
@Composable
fun EmptyState(
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    StateMessageLayout(
        iconRes = R.drawable.ic_estado_vacio,
        message = message,
        modifier = modifier,
        actionLabel = actionLabel,
        onAction = onAction,
    )
}

@PreviewLightDark
@Composable
private fun EmptyStatePreview() {
    TMTTheme(dynamicColor = false) {
        Surface {
            EmptyState(
                message = stringResource(R.string.estado_vacio_generico),
                actionLabel = stringResource(R.string.estado_limpiar_filtros),
                onAction = {},
            )
        }
    }
}
