package com.uade.tmt.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import com.uade.tmt.presentation.clubes.ClubesScreen
import com.uade.tmt.presentation.clubes.detalle.ClubDetalleScreen
import com.uade.tmt.presentation.login.LoginScreen
import com.uade.tmt.presentation.perfil.PerfilScreen
import com.uade.tmt.presentation.ranking.RankingScreen
import com.uade.tmt.presentation.reserva.ReservaScreen
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
                TorneoDetalleScreen(
                    onVolver = navController::popBackStack,
                    onInscribirme = { navController.navigate(Reserva(it)) },
                )
            }
            // Dentro de Torneos: la pestaña sigue marcada. El ViewModel vive con esta entrada,
            // así que sobrevive a la ida y vuelta al login (RF02, criterio 7).
            composable<Reserva> {
                ReservaScreen(
                    onVolver = navController::popBackStack,
                    onIrALogin = { navController.navigate(Login) },
                )
            }
        }
        composable<Ranking> { RankingScreen() }
        // Igual que Torneos: la pestaña sigue marcada en el detalle y la lista conserva su estado.
        navigation<Clubes>(startDestination = ClubesLista) {
            composable<ClubesLista> {
                ClubesScreen(onClubClick = { codigo, distancia -> navController.navigate(ClubDetalle(codigo, distancia)) })
            }
            composable<ClubDetalle> { ClubDetalleScreen(onVolver = navController::popBackStack) }
        }
        composable<Perfil> { PerfilScreen(onIniciarSesion = { navController.navigate(Login) }) }
        composable<Login> {
            LoginScreen(
                onVolver = navController::popBackStack,
                onLoginExitoso = navController::popBackStack,
            )
        }
    }
}
