package com.uade.tmt.domain.usecase

import com.uade.tmt.domain.model.ResultadoLogin
import com.uade.tmt.domain.repository.JugadorRepository
import com.uade.tmt.domain.repository.ReservaRepository
import com.uade.tmt.domain.repository.SesionRepository
import kotlinx.coroutines.flow.firstOrNull

/**
 * RF02 + RF03: como en el sitio, quien inicia sesión queda como perfil propio. No hay que buscarse
 * en el ranking ni marcarse a mano, y nadie puede hacerse pasar por otro jugador.
 *
 * Solo guarda el código: la ficha la baja Mi TMT, así el login no espera a otra página del sitio.
 * Si entra otra cuenta, el registro de inscripciones de la anterior se vacía (no son suyas).
 */
class IniciarSesion(
    private val sesiones: SesionRepository,
    private val jugadores: JugadorRepository,
    private val reservas: ReservaRepository,
) {
    suspend operator fun invoke(codigo: String, clave: String): ResultadoLogin {
        val resultado = sesiones.login(codigo, clave)
        if (resultado is ResultadoLogin.Ok) {
            val nuevo = resultado.sesion.codigo.toIntOrNull()
            val anterior = jugadores.observarCodigoPropio().firstOrNull()
            if (anterior != null && anterior != nuevo) reservas.olvidarInscripciones()
            nuevo?.let { jugadores.establecerPerfilPropio(it) }
        }
        return resultado
    }
}

/**
 * Cerrar sesión a propósito también quita el perfil propio y vacía el registro de inscripciones: el
 * teléfono puede pasar a otra persona. Si la sesión se corta sola (cookie vencida, [ReservarLugar]),
 * se mantienen.
 */
class CerrarSesion(
    private val sesiones: SesionRepository,
    private val jugadores: JugadorRepository,
    private val reservas: ReservaRepository,
) {
    suspend operator fun invoke() {
        sesiones.logout()
        jugadores.quitarPerfilPropio()
        reservas.olvidarInscripciones()
    }
}
