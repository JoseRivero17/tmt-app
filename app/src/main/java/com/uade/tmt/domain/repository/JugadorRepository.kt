package com.uade.tmt.domain.repository

import com.uade.tmt.domain.model.Jugador
import com.uade.tmt.domain.model.ResultadoSync
import com.uade.tmt.domain.model.Seguido
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/** RF03. */
interface JugadorRepository {
    fun observarRanking(division: String): Flow<List<Jugador>>

    fun observarActualizacionRanking(division: String): Flow<Instant?>

    /** Busca entre los jugadores guardados por nombre o código, sin distinguir tildes ni mayúsculas. */
    fun buscar(texto: String): Flow<List<Jugador>>

    fun observarJugador(codigo: Int): Flow<Jugador?>

    fun observarSeguidos(): Flow<List<Seguido>>

    suspend fun seguir(codigo: Int, esPropio: Boolean = false)

    suspend fun dejarDeSeguir(codigo: Int)

    /** Baja los primeros 100 jugadores de la división. */
    suspend fun sincronizarRanking(division: String, forzar: Boolean = false): ResultadoSync

    /**
     * Baja los primeros 100 de cada división, para poder buscar sin conexión. Tarda cerca de un
     * minuto o dos: se guarda división por división y la próxima vez solo se piden las que faltaron.
     */
    suspend fun precargarRanking(): ResultadoSync
}
