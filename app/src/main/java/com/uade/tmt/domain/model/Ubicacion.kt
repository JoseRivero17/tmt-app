package com.uade.tmt.domain.model

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/** Un punto del mapa. La ubicación del usuario nunca se guarda ni se envía (RNF07). */
data class Ubicacion(val latitud: Double, val longitud: Double) {

    /** Distancia en línea recta (fórmula de Haversine), en kilómetros. */
    fun distanciaKm(otra: Ubicacion): Double {
        val dLat = Math.toRadians(otra.latitud - latitud)
        val dLon = Math.toRadians(otra.longitud - longitud)
        val a = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(latitud)) * cos(Math.toRadians(otra.latitud)) * sin(dLon / 2).pow(2)
        return 2 * RADIO_TIERRA_KM * asin(sqrt(a))
    }

    private companion object {
        const val RADIO_TIERRA_KM = 6371.0
    }
}
