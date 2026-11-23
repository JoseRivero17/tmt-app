package com.uade.tmt.di

import android.content.Context
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Contenedor de dependencias de la app (inyección manual, sin Hilt ni Koin).
 *
 * Expone las dependencias que los ViewModels necesitan. Se crea una sola vez en
 * [com.uade.tmt.TMTApplication] y vive mientras viva el proceso.
 */
interface AppContainer {
    // Cada RF agrega acá su repositorio, por ejemplo:
    // val torneoRepository: TorneoRepository
}

/**
 * Implementación por defecto de [AppContainer].
 *
 * Las dependencias se crean con `by lazy`: recién se construyen la primera vez que se usan,
 * así el arranque de la app no paga el costo de objetos que todavía no hacen falta.
 */
class DefaultAppContainer(
    private val context: Context,
) : AppContainer {

    /**
     * Cliente HTTP compartido por toda la app.
     *
     * - Timeouts de 15 s para conexión, lectura y escritura (RNF03).
     * - Sin HttpLoggingInterceptor: la app maneja credenciales y no deben quedar
     *   escritas en los logs (RNF06).
     */
    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Retrofit apuntando al sitio del circuito.
     *
     * El sitio no tiene API: se pide el HTML como String (ScalarsConverterFactory) y se
     * parsea con Jsoup en la capa `data/remote`. Cada RF crea su servicio con
     * `retrofit.create(MiServicio::class.java)`.
     */
    internal val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(ScalarsConverterFactory.create())
            .build()
    }

    private companion object {
        const val BASE_URL = "https://tenisdemesaparatodos.com/"
        const val TIMEOUT_SECONDS = 15L
    }
}
