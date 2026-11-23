package com.uade.tmt.presentation.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.uade.tmt.R

/** Secciones de primer nivel que se muestran en la barra de navegación inferior. */
enum class TopLevelDestination(
    val route: Any,
    @param:StringRes val labelRes: Int,
    @param:DrawableRes val iconRes: Int,
) {
    TORNEOS(Torneos, R.string.seccion_torneos, R.drawable.ic_torneos),
    RANKING(Ranking, R.string.seccion_ranking, R.drawable.ic_ranking),
    CLUBES(Clubes, R.string.seccion_clubes, R.drawable.ic_clubes),
    PERFIL(Perfil, R.string.seccion_perfil, R.drawable.ic_perfil),
}
