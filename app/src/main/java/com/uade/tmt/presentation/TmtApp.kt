package com.uade.tmt.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.uade.tmt.presentation.navigation.TmtNavHost
import com.uade.tmt.presentation.navigation.TorneosLista
import com.uade.tmt.presentation.navigation.TopLevelDestination
import com.uade.tmt.presentation.theme.TMTTheme

/** Composable raíz: barra de navegación inferior (RNF08) + grafo de navegación. */
@Composable
fun TmtApp(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    PrecargaJugadores()

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                TopLevelDestination.entries.forEach { destination ->
                    val selected = currentDestination?.hierarchy?.any {
                        it.hasRoute(destination.route::class)
                    } == true

                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(destination.route) {
                                // Deja solo el inicio en la pila y guarda el estado de la pestaña que se abandona.
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                // Evita apilar otra copia si se vuelve a tocar la pestaña actual.
                                launchSingleTop = true
                                // Recupera el estado guardado de la pestaña destino.
                                restoreState = true
                            }
                        },
                        // El label ya lo anuncia TalkBack; el ícono es decorativo (RNF09).
                        icon = {
                            Icon(
                                painter = painterResource(destination.iconRes),
                                contentDescription = null,
                            )
                        },
                        label = {
                            // Con letra muy grande se achica lo justo para no partir la palabra (RNF09).
                            Text(
                                text = stringResource(destination.labelRes),
                                maxLines = 1,
                                autoSize = TextAutoSize.StepBased(
                                    minFontSize = 8.sp,
                                    maxFontSize = MaterialTheme.typography.labelMedium.fontSize,
                                    stepSize = 0.5.sp,
                                ),
                            )
                        },
                    )
                }
            }
        },
    ) { innerPadding ->
        // El azul del encabezado de Inicio sigue detrás de la barra de estado.
        if (currentDestination?.hasRoute<TorneosLista>() == true) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsTopHeight(WindowInsets.statusBars)
                    .background(TMTTheme.colores.marca),
            )
        }
        TmtNavHost(
            navController = navController,
            modifier = Modifier.padding(innerPadding),
        )
    }
}
