package com.uade.tmt.domain.usecase

import com.uade.tmt.domain.model.Club
import com.uade.tmt.domain.model.Ubicacion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OrdenarClubesPorCercaniaTest {

    private val ordenar = OrdenarClubesPorCercania()

    private val obelisco = Ubicacion(-34.6037, -58.3816)
    private val laPlata = Club(1, "Club La Plata", latitud = -34.9214, longitud = -57.9545)
    private val almagro = Club(2, "Club Almagro", latitud = -34.6100, longitud = -58.4200)
    private val adrogue = Club(3, "Adrogue Sith Spin", latitud = -34.796361, longitud = -58.386436)
    private val sinMapa = Club(4, "Aaa sin ubicacion")

    @Test
    fun `la distancia entre el Obelisco y La Plata es de unos 52 km`() {
        val km = obelisco.distanciaKm(Ubicacion(-34.9214, -57.9545))

        assertTrue("dio $km", km in 51.0..54.0)
        assertEquals(0.0, obelisco.distanciaKm(obelisco), 0.0)
    }

    @Test
    fun `ordena por distancia ascendente y deja al final los clubes sin ubicacion`() {
        val resultado = ordenar(listOf(laPlata, sinMapa, adrogue, almagro), obelisco)

        assertEquals(listOf(2, 3, 1, 4), resultado.map { it.club.codigo })
        assertNull(resultado.last().distanciaKm)
        assertTrue(resultado.first().distanciaKm!! < 5.0)
    }

    @Test
    fun `sin la ubicacion del usuario ordena por nombre y no muestra distancias`() {
        val resultado = ordenar(listOf(laPlata, almagro, sinMapa, adrogue), desde = null)

        assertEquals(listOf("Aaa sin ubicacion", "Adrogue Sith Spin", "Club Almagro", "Club La Plata"), resultado.map { it.club.nombre })
        assertTrue(resultado.all { it.distanciaKm == null })
    }
}
