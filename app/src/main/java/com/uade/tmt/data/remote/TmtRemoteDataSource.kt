package com.uade.tmt.data.remote

import com.uade.tmt.domain.model.Club
import com.uade.tmt.domain.model.Jugador
import com.uade.tmt.domain.model.Torneo

/**
 * Fuente remota de datos públicos. Es el único punto que sabe de dónde salen: cambiar el sitio
 * por otra fuente no toca repositorios, dominio ni presentación (RNF04).
 *
 * Los métodos lanzan IOException si falla la red y [LecturaException] si la respuesta no se entiende.
 */
interface TmtRemoteDataSource {
    suspend fun calendario(): List<Torneo>
    suspend fun torneo(codigo: Int): Torneo
    suspend fun ranking(division: String): List<Jugador>

    /** Ficha de un jugador, esté o no entre los primeros 100 de su división. Sin puesto. */
    suspend fun ficha(codigo: Int): Jugador
    suspend fun clubes(): List<Club>
}

/** La respuesta llegó, pero no tiene la estructura esperada. */
class LecturaException(mensaje: String, causa: Throwable? = null) : Exception(mensaje, causa)
