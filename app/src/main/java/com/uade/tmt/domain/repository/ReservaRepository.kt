package com.uade.tmt.domain.repository

import com.uade.tmt.domain.model.Inscripcion
import com.uade.tmt.domain.model.ResultadoOpciones
import com.uade.tmt.domain.model.ResultadoReserva
import kotlinx.coroutines.flow.Flow

/**
 * Reserva de lugar en torneos (RF02). La reserva es siempre online: sin conexión no se encola
 * (criterio 6). Ningún método suspendido lanza excepciones: todo termina en un resultado.
 */
interface ReservaRepository {

    suspend fun obtenerOpciones(torneoCodigo: Int): ResultadoOpciones

    /** Si el sitio la acepta (reservada o en lista de espera) queda en el registro local. */
    suspend fun reservar(torneoCodigo: Int, categoria: String): ResultadoReserva

    /** Reservas hechas desde la app, la más reciente primero. */
    fun observarInscripciones(): Flow<List<Inscripcion>>
}
