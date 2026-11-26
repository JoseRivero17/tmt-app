package com.uade.tmt.data.remote

import com.uade.tmt.domain.model.Jugador
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class DemoRemoteDataSourceTest {

    private val sitio = object : TmtRemoteDataSource by MuestraRemoteDataSource() {
        val pedidas = mutableListOf<Int>()
        override suspend fun ficha(codigo: Int): Jugador {
            pedidas += codigo
            return Jugador(codigo, "Real", "1ra", null, 2000)
        }
    }
    private val demo = DemoRemoteDataSource(sitio)

    @Test
    fun `la ficha del jugador demo es inventada y no se pide al sitio`() = runBlocking {
        val ficha = demo.ficha(FakeAccesoRemoteDataSource.CODIGO_DEMO.toInt())

        assertEquals(DemoRemoteDataSource.JUGADOR_DEMO, ficha)
        assertEquals(FakeAccesoRemoteDataSource.NOMBRE_DEMO, ficha.nombre)
        assertEquals(emptyList<Int>(), sitio.pedidas)
    }

    @Test
    fun `las demas fichas se leen del sitio`() = runBlocking {
        assertEquals("Real", demo.ficha(13788).nombre)
        assertEquals(listOf(13788), sitio.pedidas)
    }
}
