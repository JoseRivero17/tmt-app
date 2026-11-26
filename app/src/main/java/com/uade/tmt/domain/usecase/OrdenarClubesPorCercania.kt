package com.uade.tmt.domain.usecase

import com.uade.tmt.domain.model.Club
import com.uade.tmt.domain.model.ClubCercano
import com.uade.tmt.domain.model.Ubicacion
import com.uade.tmt.domain.model.ubicacion

/**
 * RF04, criterio 1: clubes por distancia ascendente. La distancia se calcula en el teléfono con
 * las coordenadas guardadas, así que funciona sin conexión.
 *
 * Sin ubicación del usuario (permiso rechazado o no disponible) quedan por nombre y sin distancia.
 * Los clubes que el sitio no ubicó en el mapa van al final.
 */
class OrdenarClubesPorCercania {
    operator fun invoke(clubes: List<Club>, desde: Ubicacion?): List<ClubCercano> =
        clubes
            .map { club -> ClubCercano(club, desde?.let { yo -> club.ubicacion?.distanciaKm(yo) }) }
            .sortedWith(compareBy<ClubCercano, Double?>(nullsLast()) { it.distanciaKm }.thenBy { it.club.nombre })
}
