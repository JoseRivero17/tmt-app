package com.uade.tmt.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

/** Tema de TMT Go, en claro y oscuro. No usa los colores del teléfono: la identidad es la del circuito. */
@Composable
fun TMTTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalColoresTmt provides if (darkTheme) ColoresTmtOscuro else ColoresTmtClaro) {
        MaterialTheme(
            colorScheme = if (darkTheme) EsquemaOscuro else EsquemaClaro,
            typography = Typography,
            content = content,
        )
    }
}

object TMTTheme {
    val colores: ColoresTmt
        @Composable
        @ReadOnlyComposable
        get() = LocalColoresTmt.current
}
