package com.uade.tmt.domain.usecase

import com.uade.tmt.domain.model.CategoriaTorneo
import com.uade.tmt.domain.model.FiltrosTorneo
import com.uade.tmt.domain.model.Torneo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class FiltrarTorneosTest {

    private val dia = LocalDate.of(2026, 11, 27)
    private val filtrar = FiltrarTorneos()
    private val torneos = listOf(
        torneo(1, dia, "Caballito", "7ma", "8va"),
        torneo(2, dia.plusDays(1), "Caballito", "5ta"),
        torneo(3, dia.plusDays(2), "Lanus", "7ma"),
    )

    private fun torneo(codigo: Int, fecha: LocalDate, localidad: String, vararg divisiones: String) =
        Torneo(codigo, "T$codigo", fecha, localidad, categorias = divisiones.map { CategoriaTorneo(it) })

    private fun codigos(filtros: FiltrosTorneo) = filtrar(torneos, filtros).map { it.codigo }

    @Test
    fun `sin filtros devuelve todos`() = assertEquals(listOf(1, 2, 3), codigos(FiltrosTorneo()))

    @Test
    fun `cada filtro por separado`() {
        assertEquals(listOf(1, 3), codigos(FiltrosTorneo(division = "7ma")))
        assertEquals(listOf(1, 2), codigos(FiltrosTorneo(localidad = "Caballito")))
    }

    @Test
    fun `con varios filtros deben cumplirse todos`() =
        assertEquals(listOf(1), codigos(FiltrosTorneo(division = "7ma", localidad = "Caballito")))

    @Test
    fun `el rango de fechas incluye los dos extremos`() =
        assertEquals(listOf(1, 2), codigos(FiltrosTorneo(desde = dia, hasta = dia.plusDays(1))))

    @Test
    fun `sin coincidencias devuelve una lista vacia`() =
        assertTrue(codigos(FiltrosTorneo(division = "1ra")).isEmpty())
}
