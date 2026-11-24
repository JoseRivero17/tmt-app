package com.uade.tmt.domain.repository

import com.uade.tmt.domain.model.Club
import com.uade.tmt.domain.model.ResultadoSync
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/** RF04. El orden por cercanía se calcula en el dominio: la ubicación no se guarda (RNF07). */
interface ClubRepository {
    fun observarClubes(): Flow<List<Club>>

    fun observarClub(codigo: Int): Flow<Club?>

    fun observarActualizacion(): Flow<Instant?>

    suspend fun sincronizar(forzar: Boolean = false): ResultadoSync
}
