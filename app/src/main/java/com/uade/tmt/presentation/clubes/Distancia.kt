package com.uade.tmt.presentation.clubes

import java.util.Locale
import kotlin.math.roundToInt

private val ARGENTINA = Locale.forLanguageTag("es-AR")

/** "850 m", "1,2 km", "52 km". La distancia es aproximada: no tiene sentido más precisión. */
internal fun formatoDistancia(km: Double): String = when {
    km < 1 -> "${((km * 1000) / 50).roundToInt().coerceAtLeast(1) * 50} m"
    km < 10 -> String.format(ARGENTINA, "%.1f km", km)
    else -> "${km.roundToInt()} km"
}
