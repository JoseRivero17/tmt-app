package com.uade.tmt.data.repository

import com.uade.tmt.data.remote.AccesoRemoteDataSource
import com.uade.tmt.data.remote.EstadoAcceso
import com.uade.tmt.data.remote.LecturaException
import com.uade.tmt.domain.model.ResultadoLogin
import com.uade.tmt.domain.model.SesionJugador
import com.uade.tmt.domain.repository.SesionRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import retrofit2.HttpException
import java.io.IOException

/**
 * Sesión del jugador, solo en memoria (RNF06). La clave se pasa a la fuente remota y no se guarda.
 * Ninguna falla escapa como excepción: todo termina en un [ResultadoLogin] (RNF02, RNF04).
 */
class SesionRepositoryImpl(
    private val remoto: AccesoRemoteDataSource,
) : SesionRepository {

    private val _sesion = MutableStateFlow<SesionJugador?>(null)
    override val sesion: StateFlow<SesionJugador?> = _sesion.asStateFlow()

    override suspend fun login(codigo: String, clave: String): ResultadoLogin {
        val codigoLimpio = codigo.trim()
        if (codigoLimpio.isEmpty() || clave.isEmpty()) return ResultadoLogin.DatosIncompletos

        val resultado = try {
            when (val estado = remoto.ingresar(codigoLimpio, clave)) {
                is EstadoAcceso.Exito -> ResultadoLogin.Ok(SesionJugador(codigoLimpio, estado.nombre))
                EstadoAcceso.DatosIncompletos -> ResultadoLogin.DatosIncompletos
                EstadoAcceso.CredencialesInvalidas -> ResultadoLogin.CredencialesInvalidas
                // Respuesta inesperada tras el POST (p. ej. la cookie no se guardó): no se puede
                // afirmar que la clave sea incorrecta.
                // TODO(cuenta): revisar qué devuelve el sitio con una clave incorrecta real.
                EstadoAcceso.SesionRequerida, EstadoAcceso.SinSesion -> ResultadoLogin.ErrorSitio
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            ResultadoLogin.SinConexion
        } catch (e: HttpException) {
            ResultadoLogin.ErrorSitio
        } catch (e: LecturaException) {
            ResultadoLogin.ErrorSitio
        } catch (e: Exception) {
            // Cualquier otra sorpresa del sitio o del parser: nunca crashear.
            ResultadoLogin.ErrorSitio
        }

        if (resultado is ResultadoLogin.Ok) {
            _sesion.value = resultado.sesion
        } else {
            // Tras un intento fallido no se sabe en qué estado quedaron las cookies del sitio.
            cerrarSesionLocal()
        }
        return resultado
    }

    override suspend fun logout() {
        cerrarSesionLocal()
    }

    private suspend fun cerrarSesionLocal() {
        _sesion.value = null
        try {
            remoto.cerrarSesion()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // La sesión local ya está cerrada; una falla al limpiar no debe propagarse.
        }
    }
}
