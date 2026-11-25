package com.uade.tmt.data.remote

import com.uade.tmt.data.remote.parser.AccesoParser

/**
 * Acceso real contra el sitio. La sesión la mantiene la cookie que el sitio deja en [cookies],
 * el mismo CookieJar que usa el OkHttpClient de la app.
 */
class SitioAccesoRemoteDataSource(
    private val servicio: AccesoService,
    private val cookies: CookieJarEnMemoria,
) : AccesoRemoteDataSource {

    override suspend fun ingresar(codigo: String, clave: String): EstadoAcceso =
        AccesoParser.parsear(servicio.ingresar(codigo, clave))

    // TODO(cuenta): ver si el sitio tiene una URL de salida que cierre la sesión del lado del servidor.
    override suspend fun cerrarSesion() {
        cookies.limpiar()
    }
}
