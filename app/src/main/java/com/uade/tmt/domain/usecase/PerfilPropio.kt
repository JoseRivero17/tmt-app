package com.uade.tmt.domain.usecase

import com.uade.tmt.domain.model.ResultadoLogin
import com.uade.tmt.domain.repository.JugadorRepository
import com.uade.tmt.domain.repository.SesionRepository

/**
 * RF02 + RF03: como en el sitio, quien inicia sesión queda como perfil propio. No hay que buscarse
 * en el ranking ni marcarse a mano, y nadie puede hacerse pasar por otro jugador.
 *
 * Solo guarda el código: la ficha la baja Mi TMT, así el login no espera a otra página del sitio.
 */
class IniciarSesion(
    private val sesiones: SesionRepository,
    private val jugadores: JugadorRepository,
) {
    suspend operator fun invoke(codigo: String, clave: String): ResultadoLogin {
        val resultado = sesiones.login(codigo, clave)
        if (resultado is ResultadoLogin.Ok) {
            resultado.sesion.codigo.toIntOrNull()?.let { jugadores.establecerPerfilPropio(it) }
        }
        return resultado
    }
}

/**
 * Cerrar sesión a propósito también quita el perfil propio: el teléfono puede pasar a otra persona.
 * Si la sesión se corta sola (cookie vencida, [ReservarLugar]), el perfil se mantiene.
 */
class CerrarSesion(
    private val sesiones: SesionRepository,
    private val jugadores: JugadorRepository,
) {
    suspend operator fun invoke() {
        sesiones.logout()
        jugadores.quitarPerfilPropio()
    }
}
