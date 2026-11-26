package com.uade.tmt.data.platform

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.Task
import com.uade.tmt.domain.model.Ubicacion
import com.uade.tmt.domain.repository.UbicacionProvider
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/**
 * Ubicación con el Fused Location Provider de Google Play services (RF04).
 *
 * RNF07 y RNF12: una sola lectura, que llega aproximada por el permiso, en primer plano y sin
 * seguimiento continuo. El resultado no se guarda: vive en el ViewModel mientras la pantalla está abierta.
 */
class UbicacionAndroid(private val context: Context) : UbicacionProvider {

    private val cliente by lazy { LocationServices.getFusedLocationProviderClient(context) }

    override suspend fun ubicacionActual(): Ubicacion? {
        if (!tienePermiso()) return null
        return try {
            // Si el GPS tarda o está apagado, sirve la última ubicación que conozca el sistema.
            withTimeoutOrNull(ESPERA_MAXIMA_MS) { lecturaActual() } ?: ultimaConocida()
        } catch (e: SecurityException) {
            // El usuario quitó el permiso desde Ajustes mientras la app estaba abierta.
            null
        }
    }

    private fun tienePermiso(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission") // Verificado en tienePermiso().
    private suspend fun lecturaActual(): Ubicacion? {
        val cancelacion = CancellationTokenSource()
        // Alta precisión para que el sistema pueda usar el GPS si no hay ubicación por Wi-Fi o red
        // (pasa en el emulador y en lugares sin Wi-Fi). Como la app solo tiene el permiso
        // aproximado, el resultado llega igual aproximado (RNF07). Es una sola lectura y se corta
        // a los 10 s, así que el GPS no queda encendido (RNF12).
        return cliente
            .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancelacion.token)
            .leer(onCancelar = cancelacion::cancel)
    }

    @SuppressLint("MissingPermission") // Verificado en tienePermiso().
    private suspend fun ultimaConocida(): Ubicacion? = cliente.lastLocation.leer()

    /** Convierte la Task de Play services en una función suspend, sin librerías extra. */
    private suspend fun Task<Location>.leer(onCancelar: () -> Unit = {}): Ubicacion? =
        suspendCancellableCoroutine { continuacion ->
            addOnSuccessListener { location ->
                if (continuacion.isActive) continuacion.resume(location?.let { Ubicacion(it.latitude, it.longitude) })
            }
            addOnFailureListener {
                if (continuacion.isActive) continuacion.resume(null)
            }
            continuacion.invokeOnCancellation { onCancelar() }
        }

    private companion object {
        const val ESPERA_MAXIMA_MS = 10_000L
    }
}
