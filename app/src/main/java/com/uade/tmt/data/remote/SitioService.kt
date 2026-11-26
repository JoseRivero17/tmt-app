package com.uade.tmt.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

/** Páginas públicas del sitio. Devuelven HTML: lo interpreta un parser de [com.uade.tmt.data.remote.parser]. */
interface SitioService {

    @GET("calendario.asp")
    suspend fun calendario(): String

    @GET("torneos_ampliar.asp")
    suspend fun torneo(@Query("codigo") codigo: Int): String

    /** Siempre con división: la página sin filtro tarda más que el timeout (RNF03). */
    @GET("ranking.asp")
    suspend fun ranking(@Query("fDiv") division: String): String

    @GET("jugadores_ficha.asp")
    suspend fun ficha(@Query("codigo") codigo: Int): String

    /** RF04: todos los clubes con sus coordenadas, en una sola página. */
    @GET("clubes_mapaTodos.asp")
    suspend fun clubes(): String

    @GET("clubes_ampliar.asp")
    suspend fun club(@Query("codigo") codigo: Int): String
}
