package com.uade.tmt.presentation.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Paleta del sistema de diseño en Figma (4.8): azul marino del logo y naranja de la pelotita.
// Todos los pares de texto y fondo cumplen contraste AA (RNF09).

internal val EsquemaClaro = lightColorScheme(
    primary = Color(0xFF12528F),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD5E3FF),
    onPrimaryContainer = Color(0xFF001C3A),
    secondary = Color(0xFF545F71),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD8E3F8),
    onSecondaryContainer = Color(0xFF111C2B),
    tertiary = Color(0xFF9A4600),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFDBC8),
    onTertiaryContainer = Color(0xFF321300),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFF9F9FF),
    onBackground = Color(0xFF191C20),
    surface = Color(0xFFF9F9FF),
    onSurface = Color(0xFF191C20),
    surfaceVariant = Color(0xFFE2E2E9),
    onSurfaceVariant = Color(0xFF44474E),
    surfaceDim = Color(0xFFD9D9E0),
    surfaceBright = Color(0xFFF9F9FF),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF3F3FA),
    surfaceContainer = Color(0xFFEDEDF4),
    surfaceContainerHigh = Color(0xFFE7E8EE),
    surfaceContainerHighest = Color(0xFFE2E2E9),
    outline = Color(0xFF74777F),
    outlineVariant = Color(0xFFC4C6D0),
    inverseSurface = Color(0xFF2E3036),
    inverseOnSurface = Color(0xFFF0F0F7),
    inversePrimary = Color(0xFFA6C8FF),
    scrim = Color(0xFF000000),
)

internal val EsquemaOscuro = darkColorScheme(
    primary = Color(0xFFA6C8FF),
    onPrimary = Color(0xFF00315F),
    primaryContainer = Color(0xFF00477F),
    onPrimaryContainer = Color(0xFFD5E3FF),
    secondary = Color(0xFFBCC7DB),
    onSecondary = Color(0xFF263141),
    secondaryContainer = Color(0xFF3D4758),
    onSecondaryContainer = Color(0xFFD8E3F8),
    tertiary = Color(0xFFFFB68A),
    onTertiary = Color(0xFF542100),
    tertiaryContainer = Color(0xFF783200),
    onTertiaryContainer = Color(0xFFFFDBC8),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF111318),
    onBackground = Color(0xFFE2E2E9),
    surface = Color(0xFF111318),
    onSurface = Color(0xFFE2E2E9),
    surfaceVariant = Color(0xFF44474E),
    onSurfaceVariant = Color(0xFFC4C6D0),
    surfaceDim = Color(0xFF111318),
    surfaceBright = Color(0xFF37393E),
    surfaceContainerLowest = Color(0xFF0C0E13),
    surfaceContainerLow = Color(0xFF191C20),
    surfaceContainer = Color(0xFF1D2024),
    surfaceContainerHigh = Color(0xFF282A2F),
    surfaceContainerHighest = Color(0xFF33353A),
    outline = Color(0xFF8E9099),
    outlineVariant = Color(0xFF44474E),
    inverseSurface = Color(0xFFE2E2E9),
    inverseOnSurface = Color(0xFF2E3036),
    inversePrimary = Color(0xFF12528F),
    scrim = Color(0xFF000000),
)

/** Colores propios de TMT Go que Material 3 no contempla. Se usan con `TMTTheme.colores`. */
@Immutable
data class ColoresTmt(
    /** Encabezado de Inicio. */
    val marca: Color,
    val sobreMarca: Color,
    val sobreMarcaVariante: Color,
    val pelotita: Color,
    val exitoContenedor: Color,
    val sobreExitoContenedor: Color,
    val avisoContenedor: Color,
    val sobreAvisoContenedor: Color,
)

internal val ColoresTmtClaro = ColoresTmt(
    marca = Color(0xFF003366),
    sobreMarca = Color.White,
    sobreMarcaVariante = Color(0xFFB9CCE6),
    pelotita = Color(0xFFFF8A1F),
    exitoContenedor = Color(0xFFB7F1C4),
    sobreExitoContenedor = Color(0xFF00210C),
    avisoContenedor = Color(0xFFFFDF9C),
    sobreAvisoContenedor = Color(0xFF251A00),
)

internal val ColoresTmtOscuro = ColoresTmt(
    marca = Color(0xFF0B2440),
    sobreMarca = Color.White,
    sobreMarcaVariante = Color(0xFFA9BCD6),
    pelotita = Color(0xFFFF9A3D),
    exitoContenedor = Color(0xFF005228),
    sobreExitoContenedor = Color(0xFFA9F5BA),
    avisoContenedor = Color(0xFF594400),
    sobreAvisoContenedor = Color(0xFFFFDF9C),
)

internal val LocalColoresTmt = staticCompositionLocalOf { ColoresTmtClaro }
