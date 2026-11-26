package com.uade.tmt.data.remote

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

/**
 * Cookies de la sesión del sitio, solo en memoria: no se escriben en disco y se pierden al cerrar
 * la app (RNF06). Ignora la opción "Recordar" del sitio.
 */
class CookieJarEnMemoria : CookieJar {

    private val cookies = mutableListOf<Cookie>()

    @Synchronized
    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        for (nueva in cookies) {
            this.cookies.removeAll { it.mismaQue(nueva) }
            this.cookies += nueva
        }
    }

    @Synchronized
    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val ahora = System.currentTimeMillis()
        cookies.removeAll { it.expiresAt < ahora }
        return cookies.filter { it.matches(url) }
    }

    @Synchronized
    fun limpiar() {
        cookies.clear()
    }

    private fun Cookie.mismaQue(otra: Cookie) =
        name == otra.name && domain == otra.domain && path == otra.path
}
