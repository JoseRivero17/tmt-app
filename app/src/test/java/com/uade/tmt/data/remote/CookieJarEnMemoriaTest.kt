package com.uade.tmt.data.remote

import okhttp3.Cookie
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CookieJarEnMemoriaTest {

    private val sitio = "https://tenisdemesaparatodos.com/inicio.asp".toHttpUrl()
    private val jar = CookieJarEnMemoria()

    private fun cookie(valor: String) = Cookie.parse(sitio, "ASPSESSIONID=$valor; path=/")!!

    @Test
    fun `devuelve la cookie guardada y la reemplaza si llega otra con el mismo nombre`() {
        jar.saveFromResponse(sitio, listOf(cookie("uno")))
        jar.saveFromResponse(sitio, listOf(cookie("dos")))

        assertEquals(listOf("dos"), jar.loadForRequest(sitio).map { it.value })
    }

    @Test
    fun `no manda cookies a otro dominio`() {
        jar.saveFromResponse(sitio, listOf(cookie("uno")))

        assertTrue(jar.loadForRequest("https://otro.com/".toHttpUrl()).isEmpty())
    }

    @Test
    fun `limpiar borra todas las cookies`() {
        jar.saveFromResponse(sitio, listOf(cookie("uno")))

        jar.limpiar()

        assertTrue(jar.loadForRequest(sitio).isEmpty())
    }
}
