package com.uade.tmt.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.uade.tmt.TMTApplication

/**
 * Deja jugadores de todas las divisiones guardados para poder buscar sin conexión (RF03).
 *
 * Solo con la app a la vista: al pasar a segundo plano se cancela y al volver pide las divisiones
 * que faltaban (RNF12). Cada división se pide como máximo una vez por día (RNF05).
 */
@Composable
internal fun PrecargaJugadores() {
    val app = LocalContext.current.applicationContext as TMTApplication
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            app.container.jugadorRepository.precargarRanking()
        }
    }
}
