package com.uade.tmt.presentation.clubes.detalle

import com.uade.tmt.domain.model.Club
import java.net.URLEncoder

/**
 * "Cómo llegar" (RF04, criterio 3): URI `geo:` que abre la app de mapas del teléfono.
 *
 * Usa las coordenadas del sitio con el nombre del club como etiqueta: son más precisas que la
 * dirección, que a veces trae aclaraciones ("entre Drumond y plaza"). Si el club no está en el
 * mapa, busca por dirección y localidad. Null si no hay ni lo uno ni lo otro.
 */
internal fun uriComoLlegar(club: Club): String? {
    if (club.latitud != null && club.longitud != null) {
        return "geo:${club.latitud},${club.longitud}?q=${club.latitud},${club.longitud}(${codificar(club.nombre)})"
    }
    val direccion = listOfNotNull(club.direccion, club.localidad).joinToString(", ")
    return if (club.direccion != null) "geo:0,0?q=${codificar(direccion)}" else null
}

/** Para cuando no hay app que entienda `geo:`: Google Maps en el navegador. */
internal fun urlMapaWeb(club: Club): String? {
    val consulta = if (club.latitud != null && club.longitud != null) {
        "${club.latitud},${club.longitud}"
    } else {
        club.direccion?.let { listOfNotNull(it, club.localidad).joinToString(", ") } ?: return null
    }
    return "https://www.google.com/maps/search/?api=1&query=${codificar(consulta)}"
}

/** "Llamar": abre el marcador con el número cargado; no hace falta permiso de teléfono. */
internal fun uriLlamar(club: Club): String? =
    club.telefono?.filter { it.isDigit() || it == '+' }?.takeIf { it.length >= 6 }?.let { "tel:$it" }

private fun codificar(texto: String): String = URLEncoder.encode(texto, "UTF-8")
