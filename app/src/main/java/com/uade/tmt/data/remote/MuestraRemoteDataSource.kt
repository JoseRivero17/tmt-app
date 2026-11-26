package com.uade.tmt.data.remote

import com.uade.tmt.domain.model.CategoriaTorneo
import com.uade.tmt.domain.model.Club
import com.uade.tmt.domain.model.Jugador
import com.uade.tmt.domain.model.Torneo
import java.time.Clock
import java.time.LocalDate

/**
 * Datos INVENTADOS para desarrollar las pantallas sin depender del sitio. Se reemplaza por la
 * implementación real en [com.uade.tmt.di.DefaultAppContainer].
 */
class MuestraRemoteDataSource(private val clock: Clock = Clock.systemDefaultZone()) : TmtRemoteDataSource {

    override suspend fun calendario(): List<Torneo> {
        val hoy = LocalDate.now(clock)
        return listOf(
            Torneo(1, "Torneo de la Semana", hoy.plusDays(2), "VILLA URQUIZA", inscripcionOnline = true),
            Torneo(2, "Copa Primavera", hoy.plusDays(4), "LANUS", inscripcionOnline = true),
            Torneo(3, "Abierto del Oeste", hoy.plusDays(4), "MORON", cancelado = true, inscripcionOnline = true),
            Torneo(4, "Nocturno de los Jueves", hoy.plusDays(9), "CABALLITO"),
            Torneo(5, "Gran Final del Circuito", hoy.plusDays(16), "LA PLATA", inscripcionOnline = true),
        )
    }

    override suspend fun torneo(codigo: Int): Torneo {
        val base = calendario().firstOrNull { it.codigo == codigo }
            ?: throw LecturaException("No existe el torneo $codigo")
        return base.copy(
            clubCodigo = 1,
            sede = "Club de Muestra",
            direccion = "Calle Inventada 123",
            categorias = listOf(
                CategoriaTorneo("5ta división", "3 sets, final 5 sets", "19:00", 24),
                CategoriaTorneo("7ma división", "3 sets, final 5 sets", "20:30", 18),
            ),
        )
    }

    override suspend fun ranking(division: String): List<Jugador> = (1..12).map { puesto ->
        Jugador(
            codigo = division.hashCode().mod(800) * 100 + puesto,
            nombre = "JUGADOR DE MUESTRA $puesto",
            division = division,
            puesto = puesto,
            rating = 1500 - puesto * 12,
            variacion = puesto % 5 - 2,
            club = if (puesto % 2 == 0) "Club de Muestra" else "Otro Club",
            provincia = "Buenos Aires",
        )
    }

    override suspend fun ficha(codigo: Int): Jugador =
        Jugador(codigo, "JUGADOR DE MUESTRA", "7ma", puesto = null, rating = 1000, club = "Club de Muestra")

    override suspend fun clubes(): List<Club> = listOf(
        Club(1, "Club de Muestra", "Calle Inventada 123", "CABA", "11 0000-0000", latitud = -34.6037, longitud = -58.3816),
        Club(2, "Otro Club", "Avenida Ficticia 456", "LANUS", latitud = -34.7002, longitud = -58.3920),
        Club(3, "Club sin ubicación", "Ruta Imaginaria 789", "LA PLATA"),
    )

    override suspend fun club(codigo: Int): Club =
        clubes().firstOrNull { it.codigo == codigo } ?: throw LecturaException("No existe el club $codigo")
}
