package com.uade.tmt.data.mapper

import com.uade.tmt.data.local.entity.ClubEntity
import com.uade.tmt.data.local.entity.JugadorConClub
import com.uade.tmt.data.local.entity.TorneoConCategorias
import com.uade.tmt.domain.model.CategoriaTorneo
import com.uade.tmt.domain.model.Jugador
import com.uade.tmt.domain.model.Torneo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class MappersTest {

    @Test
    fun `la busqueda no distingue tildes, mayusculas ni espacios`() {
        assertEquals("pena angel", "  PEÑA   Ángel ".normalizado())
    }

    @Test
    fun `un torneo se guarda y se recupera igual, con sus categorias en orden`() {
        val torneo = Torneo(
            codigo = 11,
            nombre = "Copa",
            fecha = LocalDate.of(2026, 11, 27),
            localidad = "CABA",
            clubCodigo = 9,
            club = "Club",
            sede = "Sede",
            direccion = "Calle 1",
            precios = "$1000",
            inscripcionOnline = true,
            categorias = listOf(
                CategoriaTorneo("7ma", "3 sets", "20:30", 18),
                CategoriaTorneo("8va", null, "21:00", null),
            ),
        )
        val club = ClubEntity(9, "Club", null, null, null, null, null, null)
        val guardado = TorneoConCategorias(torneo.aEntidad(), torneo.categoriasAEntidad().reversed(), club)

        assertEquals(torneo, guardado.aDominio())
    }

    @Test
    fun `un jugador guardado conserva sus datos y la fecha de actualizacion`() {
        val ahora = Instant.parse("2026-11-24T18:00:00Z")
        val jugador = Jugador(codigo = 5, nombre = "Núñez", division = "5ta", puesto = 3, rating = 1400)

        val entidad = jugador.aEntidad(ahora)

        assertEquals("nunez", entidad.nombreBusqueda)
        assertEquals(jugador.copy(actualizadoEn = ahora), JugadorConClub(entidad, club = null).aDominio())
    }

    @Test
    fun `el jugador muestra el nombre del club y, si no se conoce, la abreviatura`() {
        val entidad = Jugador(5, "Ana", "5ta", 3, 1400, clubCodigo = 9, club = "CLU").aEntidad(Instant.EPOCH)
        val club = ClubEntity(9, "Club Nueve", null, null, null, null, null, null)

        assertEquals("Club Nueve", JugadorConClub(entidad, club).aDominio().club)
        assertEquals("CLU", JugadorConClub(entidad, null).aDominio().club)
    }

    @Test
    fun `un torneo cancelado no admite reserva aunque tenga inscripcion online`() {
        val torneo = Torneo(1, "Copa", LocalDate.of(2026, 11, 27), "CABA", inscripcionOnline = true)

        assertTrue(torneo.admiteReserva)
        assertFalse(torneo.copy(cancelado = true).admiteReserva)
    }
}
