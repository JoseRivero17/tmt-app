package com.uade.tmt.presentation.ranking

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** RF03: la búsqueda arranca recién con 3 caracteres. */
class RankingUiStateTest {

    @Test
    fun `con menos de 3 caracteres no busca`() {
        assertFalse(RankingUiState(busqueda = "").buscando)
        assertFalse(RankingUiState(busqueda = "ma").buscando)
    }

    @Test
    fun `con 3 caracteres o mas busca`() {
        assertTrue(RankingUiState(busqueda = "mar").buscando)
        assertTrue(RankingUiState(busqueda = "martin").buscando)
    }

    @Test
    fun `los espacios no cuentan como caracteres`() {
        assertFalse(RankingUiState(busqueda = "  ma  ").buscando)
    }

    @Test
    fun `busca tambien por codigo`() {
        assertTrue(RankingUiState(busqueda = "123").buscando)
    }
}