package com.uade.tmt.data.remote

import kotlinx.coroutines.delay
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Acceso inventado para desarrollar el RF02 sin cuenta en el sitio. Acepta solo el jugador de
 * demostración [CODIGO_DEMO] / [CLAVE_DEMO]. La clave es inventada, pero el código existe en el
 * sitio: al ingresar, Mi TMT muestra la ficha pública de ese jugador como perfil propio.
 */
class FakeAccesoRemoteDataSource(
    private val demora: Duration = 600.milliseconds,
) : AccesoRemoteDataSource {

    override suspend fun ingresar(codigo: String, clave: String): EstadoAcceso {
        delay(demora)
        return when {
            codigo.isBlank() || clave.isBlank() -> EstadoAcceso.DatosIncompletos
            codigo == CODIGO_DEMO && clave == CLAVE_DEMO -> EstadoAcceso.Exito(nombre = NOMBRE_DEMO)
            else -> EstadoAcceso.CredencialesInvalidas
        }
    }

    override suspend fun cerrarSesion() = Unit

    companion object {
        const val CODIGO_DEMO = "12345"
        const val CLAVE_DEMO = "demo"
        const val NOMBRE_DEMO = "Jugador Demo"
    }
}
