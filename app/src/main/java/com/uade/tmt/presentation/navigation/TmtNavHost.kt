package com.uade.tmt.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import com.uade.tmt.presentation.clubes.ClubesScreen
import com.uade.tmt.presentation.perfil.PerfilScreen
import com.uade.tmt.presentation.ranking.FichaJugadorScreen
import com.uade.tmt.presentation.ranking.RankingScreen
import com.uade.tmt.presentation.torneos.TorneosScreen
import com.uade.tmt.presentation.torneos.detalle.TorneoDetalleScreen

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
        // Sección con detalle: la pestaña sigue marcada en el detalle y conserva su estado.
        navigation<Torneos>(startDestination = TorneosLista) {
            composable<TorneosLista> {
                TorneosScreen(onTorneoClick = { navController.navigate(TorneoDetalle(it)) })
            }
            composable<TorneoDetalle> {
                TorneoDetalleScreen(onVolver = navController::popBackStack)
            }
        }

        navigation<Ranking>(startDestination = RankingLista) {
            composable<RankingLista> {
                RankingScreen(onJugadorClick = { navController.navigate(JugadorFicha(it)) })
            }
            composable<JugadorFicha> {
                FichaJugadorScreen(onVolver = navController::popBackStack)
            }
        }

        composable<Clubes> { ClubesScreen() }
        composable<Perfil> { PerfilScreen() }
    }
}
