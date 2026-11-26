package com.uade.tmt.domain.usecase

import com.uade.tmt.domain.model.OpcionInscripcion
import com.uade.tmt.domain.model.ResultadoReserva
import com.uade.tmt.domain.repository.ReservaRepository
import com.uade.tmt.domain.repository.SesionRepository

/**
 * RF02: valida todo lo que se puede validar localmente antes de ir al sitio, en este orden:
 * sesión activa, condiciones aceptadas y categoría habilitada.
 *
 * Sin sesión devuelve [ResultadoReserva.SesionRequerida] sin consultar el sitio: la UI guarda
 * torneo y categoría, manda al login y, al volver, llama otra vez a este caso de uso (criterio 7).
 */
class ReservarLugar(
    private val sesiones: SesionRepository,
    private val reservas: ReservaRepository,
) {
    suspend operator fun invoke(
        torneoCodigo: Int,
        opcion: OpcionInscripcion,
        condicionesAceptadas: Boolean,
    ): ResultadoReserva {
        if (sesiones.sesion.value == null) return ResultadoReserva.SesionRequerida
        if (!condicionesAceptadas) return ResultadoReserva.Rechazada(MOTIVO_CONDICIONES)
        if (!opcion.habilitada) {
            return ResultadoReserva.Rechazada(opcion.motivo ?: MOTIVO_CATEGORIA)
        }

        val resultado = reservas.reservar(torneoCodigo, opcion.categoria)
        if (resultado == ResultadoReserva.SesionRequerida) {
            // La sesión local existía pero el sitio no la reconoce (cookie vencida): se cierra
            // para que la UI pida el login de nuevo y retome la reserva.
            sesiones.logout()
        }
        return resultado
    }

    companion object {
        const val MOTIVO_CONDICIONES = "Tenés que aceptar las condiciones del torneo"
        const val MOTIVO_CATEGORIA = "Esta categoría no está habilitada"
    }
}
