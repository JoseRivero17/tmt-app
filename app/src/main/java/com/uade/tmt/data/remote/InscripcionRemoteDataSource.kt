package com.uade.tmt.data.remote

import com.uade.tmt.domain.model.ResultadoOpciones
import com.uade.tmt.domain.model.ResultadoReserva

/**
 * Fuente remota de la inscripción a torneos (RF02). Hay una real ([SitioInscripcionRemoteDataSource])
 * y una falsa ([FakeInscripcionRemoteDataSource]); el AppContainer elige según `BuildConfig.SITIO_REAL`.
 *
 * Los métodos lanzan IOException si falla la red y [LecturaException] si la respuesta no se
 * entiende; el repositorio los traduce a resultados. Nunca devuelven `SinConexion`.
 */
interface InscripcionRemoteDataSource {
    suspend fun opciones(torneoCodigo: Int): ResultadoOpciones

    suspend fun reservar(torneoCodigo: Int, categoria: String): ResultadoReserva
}
