package com.uade.tmt.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Inscripciones online del sitio. Devuelve HTML: lo interpreta
 * [com.uade.tmt.data.remote.parser.InscripcionParser]. La sesión viaja en las cookies del CookieJar.
 *
 * TODO(cuenta): agregar el POST del formulario cuando se conozcan su `action` y sus campos
 * (ver docs/rf02-pendiente-cuenta.md). No se inventa un endpoint que no se vio.
 */
interface InscripcionService {

    @GET("jugadores_inscripciones.asp")
    suspend fun formulario(@Query("torneo_cod") torneoCodigo: Int): String
}
