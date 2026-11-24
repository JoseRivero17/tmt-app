package com.uade.tmt.domain.repository

import com.uade.tmt.domain.model.ResultadoSync
import com.uade.tmt.domain.model.Torneo
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/** RF01. Las lecturas salen siempre de lo guardado; `sincronizar*` es lo único que usa la red. */
interface TorneoRepository {
    /** Torneos desde hoy, por fecha. */
    fun observarCalendario(): Flow<List<Torneo>>

    fun observarTorneo(codigo: Int): Flow<Torneo?>

    /** Fecha de la última actualización del calendario, o null si nunca se sincronizó (RNF01). */
    fun observarActualizacionCalendario(): Flow<Instant?>

    fun observarActualizacionTorneo(codigo: Int): Flow<Instant?>

    suspend fun sincronizarCalendario(forzar: Boolean = false): ResultadoSync

    suspend fun sincronizarTorneo(codigo: Int, forzar: Boolean = false): ResultadoSync
}
