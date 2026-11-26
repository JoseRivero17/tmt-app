package com.uade.tmt.domain.repository

import com.uade.tmt.domain.model.Ubicacion

/**
 * Ubicación aproximada del teléfono (RF04). Aísla a Google Play services: el dominio y los
 * ViewModels se prueban sin Android. El permiso lo pide la pantalla, en contexto (RNF07).
 */
interface UbicacionProvider {
    /** Una sola lectura, sin seguimiento. Null si no hay permiso o no se pudo obtener. */
    suspend fun ubicacionActual(): Ubicacion?
}
