package com.uade.tmt.data.remote

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import com.uade.tmt.domain.repository.Conectividad
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged

/** Escucha al sistema mientras alguien observa [conectado]; al dejar de hacerlo, se da de baja (RNF12). */
class ConectividadAndroid(context: Context) : Conectividad {

    private val manager = context.getSystemService(ConnectivityManager::class.java)

    override val conectado: Flow<Boolean> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                trySend(capabilities.tieneInternet())
            }

            override fun onLost(network: Network) {
                trySend(false)
            }
        }
        // Si no hay red el sistema no llama al callback: el primer valor se consulta acá.
        trySend(manager.getNetworkCapabilities(manager.activeNetwork)?.tieneInternet() == true)
        manager.registerDefaultNetworkCallback(callback)
        awaitClose { manager.unregisterNetworkCallback(callback) }
    }.distinctUntilChanged().conflate()

    /** VALIDATED descarta el Wi-Fi conectado pero sin salida a internet. */
    private fun NetworkCapabilities.tieneInternet() =
        hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
}
