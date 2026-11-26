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

    suspend fun seguir(codigo: Int)

    suspend fun dejarDeSeguir(codigo: Int)

    /** Código del perfil propio, aunque su ficha todavía no se haya bajado. */
    fun observarCodigoPropio(): Flow<Int?>

    /**
     * El perfil propio es el del jugador que inició sesión (RF02), como en el sitio: no se elige a
     * mano. Reemplaza al anterior, si había otro.
     */
    suspend fun establecerPerfilPropio(codigo: Int)

    /** Al cerrar sesión el teléfono deja de tener perfil propio. */
    suspend fun quitarPerfilPropio()

    /** Baja la ficha de un jugador, que puede no estar entre los primeros 100 de su división. */
    suspend fun sincronizarFicha(codigo: Int, forzar: Boolean = false): ResultadoSync

    /** [sincronizarFicha] del perfil propio, si hay uno. */
    suspend fun sincronizarPerfilPropio(forzar: Boolean = false): ResultadoSync

    /** Baja los primeros 100 jugadores de la división. */
    suspend fun sincronizarRanking(division: String, forzar: Boolean = false): ResultadoSync

    /**
     * Baja los primeros 100 de cada división, para poder buscar sin conexión. Tarda cerca de un
     * minuto o dos: se guarda división por división y la próxima vez solo se piden las que faltaron.
     */
    suspend fun precargarRanking(): ResultadoSync
}
