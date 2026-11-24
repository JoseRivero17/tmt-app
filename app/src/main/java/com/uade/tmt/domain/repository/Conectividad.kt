package com.uade.tmt.domain.repository

import kotlinx.coroutines.flow.Flow

/** Estado de la conexión del teléfono. RF02 la usa para deshabilitar la reserva sin red. */
interface Conectividad {
    val conectado: Flow<Boolean>
}
