package com.uade.tmt.data.remote

/**
 * Fuente remota del acceso de jugadores (RF02). Hay una real ([SitioAccesoRemoteDataSource]) y una
 * falsa ([FakeAccesoRemoteDataSource]); el AppContainer elige cuál usar según `BuildConfig.SITIO_REAL`.
 *
 * [ingresar] lanza IOException si falla la red y [LecturaException] si la respuesta no se entiende.
 */
interface AccesoRemoteDataSource {
    suspend fun ingresar(codigo: String, clave: String): EstadoAcceso

    /** Olvida la sesión del sitio (cookies). */
    suspend fun cerrarSesion()
}

/** Qué pantalla devolvió el sitio, según [com.uade.tmt.data.remote.parser.AccesoParser]. */
sealed interface EstadoAcceso {
    /** Página pública: el encabezado tiene el formulario de código y clave. */
    data object SinSesion : EstadoAcceso

    /** "ACCESO DE JUGADORES" con "Debes completar todos tus datos de acceso". */
    data object DatosIncompletos : EstadoAcceso

    /** "ACCESO DE JUGADORES" con "Debes ingresar como usuario…": la sección pide sesión. */
    data object SesionRequerida : EstadoAcceso

    /** "ACCESO DE JUGADORES" con cualquier otro mensaje. */
    data object CredencialesInvalidas : EstadoAcceso

    data class Exito(val nombre: String?) : EstadoAcceso
}
