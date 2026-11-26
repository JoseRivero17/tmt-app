package com.uade.tmt.presentation.clubes

import org.junit.Assert.assertEquals
import org.junit.Test

class DistanciaTest {

    @Test
    fun `menos de un kilometro se muestra en metros redondeados`() {
        assertEquals("850 m", formatoDistancia(0.84))
        assertEquals("50 m", formatoDistancia(0.004))
    }

    @Test
    fun `hasta diez kilometros con un decimal y coma`() {
        assertEquals("1,2 km", formatoDistancia(1.234))
    }

    @Test
    fun `lejos sin decimales`() {
        assertEquals("52 km", formatoDistancia(52.4))
    }
}
