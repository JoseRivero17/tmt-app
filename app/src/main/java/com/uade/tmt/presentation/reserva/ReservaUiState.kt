package com.uade.tmt.presentation.reserva

import com.uade.tmt.domain.model.OpcionInscripcion
import com.uade.tmt.domain.model.ResultadoReserva

enum class CargaOpciones { CARGANDO, LISTA, SIN_CONEXION, ERROR_SITIO, SESION_REQUERIDA }

/** Qué se retoma cuando aparece la sesión después del login (criterio 7). */
enum class Pendiente { OPCIONES, RESERVA }

data class ReservaUiState(
    val torneoCodigo: Int,
    val carga: CargaOpciones = CargaOpciones.CARGANDO,
    val opciones: List<OpcionInscripcion> = emptyList(),
    val seleccionada: String? = null,
    val condicionesAceptadas: Boolean = false,
    /** Según Conectividad. Sin red no se puede confirmar y nada se encola (criterio 6). */
    val conectado: Boolean = true,
    val enviando: Boolean = false,
    /** Último resultado de ReservarLugar, salvo SesionRequerida (esa se convierte en [pendiente]). */
    val resultado: ResultadoReserva? = null,
    val pendiente: Pendiente? = null,
    /** Evento de un solo uso: la UI navega al login y llama a `loginAbierto()`. */
    val irALogin: Boolean = false,
) {
    val opcionElegida: OpcionInscripcion? get() = opciones.find { it.categoria == seleccionada }

    /** Resultados finales: ya no se muestra el formulario. Incierta incluida, para no duplicar. */
    val terminada: Boolean
        get() = resultado == ResultadoReserva.Reservada ||
            resultado == ResultadoReserva.ListaEspera ||
            resultado == ResultadoReserva.Incierta

    val puedeConfirmar: Boolean
        get() = carga == CargaOpciones.LISTA &&
            opcionElegida?.habilitada == true &&
            condicionesAceptadas &&
            conectado &&
            !enviando &&
            !terminada

    val ofreceReintentar: Boolean
        get() = resultado == ResultadoReserva.SinConexion || resultado == ResultadoReserva.ErrorSitio

    /** Faltaba la sesión para enviar y el jugador volvió del login sin ingresar. */
    val pideLogin: Boolean get() = pendiente == Pendiente.RESERVA && !irALogin
}
