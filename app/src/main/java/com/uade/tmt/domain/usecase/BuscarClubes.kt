package com.uade.tmt.domain.usecase

import com.uade.tmt.domain.model.Club
import java.text.Normalizer

/**
 * RF04, criterio 2: búsqueda por localidad (o por nombre), la alternativa cuando no hay permiso
 * de ubicación. Ignora mayúsculas y tildes: "adrogue" encuentra "Adrogué".
 */
class BuscarClubes {
    operator fun invoke(clubes: List<Club>, texto: String): List<Club> {
        val buscado = texto.comparable()
        if (buscado.isEmpty()) return clubes
        return clubes.filter { club ->
            club.nombre.comparable().contains(buscado) || club.localidad.orEmpty().comparable().contains(buscado)
        }
    }

    private fun String.comparable(): String =
        Normalizer.normalize(this, Normalizer.Form.NFD)
            .replace(TILDES, "")
            .lowercase()
            .trim()
            .replace(ESPACIOS, " ")

    private companion object {
        val TILDES = Regex("\\p{Mn}+")
        val ESPACIOS = Regex("\\s+")
    }
}
