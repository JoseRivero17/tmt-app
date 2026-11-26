package com.uade.tmt.domain.usecase

import com.uade.tmt.domain.model.Club
import org.junit.Assert.assertEquals
import org.junit.Test

class BuscarClubesTest {

    private val buscar = BuscarClubes()
    private val clubes = listOf(
        Club(1, "Club Social Villa Crespo", localidad = "Villa Crespo"),
        Club(2, "Ping Pong Corrientes", localidad = "Villa Crespo"),
        Club(3, "Adrogue Sith Spin", localidad = "Adrogué"),
        Club(4, "Club sin localidad"),
    )

    @Test
    fun `busca por localidad sin importar mayusculas ni tildes`() {
        assertEquals(listOf(1, 2), buscar(clubes, "villa crespo").map { it.codigo })
        assertEquals(listOf(3), buscar(clubes, "ADROGUE").map { it.codigo })
    }

    @Test
    fun `tambien busca por nombre del club`() {
        assertEquals(listOf(2), buscar(clubes, "corrientes").map { it.codigo })
    }

    @Test
    fun `sin texto devuelve todos`() {
        assertEquals(clubes, buscar(clubes, "  "))
    }

    @Test
    fun `sin coincidencias devuelve una lista vacia`() {
        assertEquals(emptyList<Club>(), buscar(clubes, "ushuaia"))
    }
}
