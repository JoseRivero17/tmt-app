package com.uade.tmt.data.remote

import kotlinx.coroutines.runBlocking
import okhttp3.Cookie
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class SitioAccesoRemoteDataSourceTest {

    private val sitio = "https://tenisdemesaparatodos.com/".toHttpUrl()
    private val cookies = CookieJarEnMemoria().apply {
        saveFromResponse(sitio, listOf(Cookie.parse(sitio, "ASPSESSIONID=abc; path=/")!!))
    }

    private class ServicioFalso(private val falla: Boolean = false) : AccesoService {
        var salidas = 0
        override suspend fun ingresar(codigo: String, clave: String) = error("no se usa")
        override suspend fun salir(): String {
            salidas++
            if (falla) throw IOException()
            return ""
        }
    }

    @Test
    fun `cerrar sesion avisa al sitio y borra las cookies`() = runBlocking {
        val servicio = ServicioFalso()

        SitioAccesoRemoteDataSource(servicio, cookies).cerrarSesion()

        assertEquals(1, servicio.salidas)
        assertTrue(cookies.loadForRequest(sitio).isEmpty())
    }

    @Test
    fun `si el aviso al sitio falla las cookies se borran igual`() {
        val acceso = SitioAccesoRemoteDataSource(ServicioFalso(falla = true), cookies)

        assertThrows(IOException::class.java) { runBlocking { acceso.cerrarSesion() } }
        assertTrue(cookies.loadForRequest(sitio).isEmpty())
    }
}
