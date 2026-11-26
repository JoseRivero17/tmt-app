package com.uade.tmt.data.remote

import retrofit2.http.FieldMap
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * Inscripciones online del sitio. Devuelve HTML: lo interpreta
 * [com.uade.tmt.data.remote.parser.InscripcionParser]. La sesión viaja en las cookies del CookieJar.
 */
interface InscripcionService {

    @GET("jugadores_inscripciones.asp")
    suspend fun formulario(@Query("torneo_cod") torneoCodigo: Int): String

    /**
     * El envío del popup "Confirmar inscripción" (visto con una cuenta real, sin enviarlo). INSCRIBE
     * DE VERDAD: solo se llama con `enviarAlSitio` en [SitioInscripcionRemoteDataSource].
     */
    @FormUrlEncoded
    @POST("jugadores_inscripciones_paso2.asp")
    suspend fun enviar(@FieldMap campos: Map<String, String>): String
}
