package com.uade.tmt.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.uade.tmt.presentation.clubes.ClubesScreen
import com.uade.tmt.presentation.perfil.PerfilScreen
import com.uade.tmt.presentation.ranking.RankingScreen
import com.uade.tmt.presentation.torneos.TorneosScreen

@Composable
fun TmtNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = Torneos,
        modifier = modifier,
    ) {
        composable<Torneos> { TorneosScreen() }
        composable<Ranking> { RankingScreen() }
        composable<Clubes> { ClubesScreen() }
        composable<Perfil> { PerfilScreen() }
    }
}
