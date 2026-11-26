package com.uade.tmt.data.remote

import kotlinx.coroutines.delay
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Acceso inventado para desarrollar y mostrar el RF02 sin usar una cuenta real. Acepta solo el
 * jugador de demostración [CODIGO_DEMO] / [CLAVE_DEMO], que no existe en el sitio (el sitio
 * redirige esa ficha), así que nunca se confunde con un jugador real. Su ficha la da
 * [DemoRemoteDataSource].
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
        const val CODIGO_DEMO = "99999"
        const val CLAVE_DEMO = "demo"
        const val NOMBRE_DEMO = "Jugador Demo"
    }
}
