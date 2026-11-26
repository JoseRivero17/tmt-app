package com.uade.tmt.data.remote

import com.uade.tmt.domain.model.OpcionInscripcion
import com.uade.tmt.domain.model.ResultadoOpciones
import com.uade.tmt.domain.model.ResultadoReserva
import com.uade.tmt.domain.model.Torneo
import com.uade.tmt.domain.repository.TorneoRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Inscripción inventada para desarrollar el RF02 sin cuenta en el sitio. Nunca hace requests:
 * arma las opciones con las categorías del torneo ya guardado (RF01) y responde siempre igual.
 *
 * - La primera categoría está deshabilitada con [MOTIVO_RATING].
 * - Reservar la última devuelve lista de espera; cualquier otra habilitada, reservada.
 * - Si el torneo no admite reserva, todas están deshabilitadas.
 */
class FakeInscripcionRemoteDataSource(
    private val torneos: TorneoRepository,
    private val demora: Duration = 600.milliseconds,
) : InscripcionRemoteDataSource {

    override suspend fun opciones(torneoCodigo: Int): ResultadoOpciones {
        delay(demora)
        val torneo = torneos.observarTorneo(torneoCodigo).first()
            ?: return ResultadoOpciones.ErrorSitio
        return ResultadoOpciones.Disponibles(opcionesDe(torneo))
    }

    override suspend fun reservar(torneoCodigo: Int, categoria: String): ResultadoReserva {
        delay(demora)
        val torneo = torneos.observarTorneo(torneoCodigo).first()
            ?: return ResultadoReserva.ErrorSitio
        val opciones = opcionesDe(torneo)
        val opcion = opciones.find { it.categoria == categoria }
            ?: return ResultadoReserva.Rechazada(MOTIVO_SIN_CATEGORIA)

        return when {
            !opcion.habilitada -> ResultadoReserva.Rechazada(opcion.motivo ?: MOTIVO_SIN_CATEGORIA)
            opcion == opciones.last() -> ResultadoReserva.ListaEspera
            else -> ResultadoReserva.Reservada
        }
    }

    private fun opcionesDe(torneo: Torneo): List<OpcionInscripcion> =
        torneo.categorias.mapIndexed { indice, categoria ->
            val motivo = when {
                !torneo.admiteReserva -> MOTIVO_SIN_RESERVA
                indice == 0 -> MOTIVO_RATING
                else -> null
            }
            OpcionInscripcion(
                categoria = categoria.division,
                horario = categoria.hora,
                habilitada = motivo == null,
                motivo = motivo,
            )
        }

    companion object {
        const val MOTIVO_RATING = "Tu rating no habilita esta categoría"
        const val MOTIVO_SIN_RESERVA = "El torneo no admite inscripción online"
        const val MOTIVO_SIN_CATEGORIA = "El torneo no tiene esa categoría"
    }
}
