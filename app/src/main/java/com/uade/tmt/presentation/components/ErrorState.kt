package com.uade.tmt.presentation.components

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.uade.tmt.R
import com.uade.tmt.presentation.theme.TMTTheme

/**
 * Error que impide mostrar la pantalla (RNF02): mensaje comprensible para el usuario,
 * sin detalles técnicos, y botón "Reintentar".
 */
@Composable
fun ErrorState(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    message: String = stringResource(R.string.estado_error_generico),
) {
    StateMessageLayout(
        iconRes = R.drawable.ic_estado_error,
        message = message,
        modifier = modifier,
        actionLabel = stringResource(R.string.estado_reintentar),
        onAction = onRetry,
    )
}

@PreviewLightDark
@Composable
private fun ErrorStatePreview() {
    TMTTheme {
        Surface {
            ErrorState(onRetry = {})
        }
    }
}
