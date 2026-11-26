package com.uade.tmt.data.remote

import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

/**
 * Acceso de jugadores del sitio. Devuelve HTML: lo interpreta
 * [com.uade.tmt.data.remote.parser.AccesoParser].
 *
 * El código y la clave viajan en el cuerpo del POST, nunca en la URL (RNF06). El campo `recordar`
 * no se envía nunca. OkHttp sigue la redirección a `accesoURL` con las cookies del CookieJar.
 */
interface AccesoService {

    @FormUrlEncoded
    @POST("jugadores_acceso_paso2.asp?accion=ingresar&accesoURL=inicio.asp")
    suspend fun ingresar(
        @Field("codigo") codigo: String,
        @Field("clave") clave: String,
    ): String
}
