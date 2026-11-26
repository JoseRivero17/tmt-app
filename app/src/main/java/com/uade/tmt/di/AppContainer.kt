package com.uade.tmt.di

import android.content.Context
import com.uade.tmt.BuildConfig
import com.uade.tmt.data.local.AppDatabase
import com.uade.tmt.data.remote.AccesoRemoteDataSource
import com.uade.tmt.data.remote.AccesoService
import com.uade.tmt.data.remote.ConectividadAndroid
import com.uade.tmt.data.remote.CookieJarEnMemoria
import com.uade.tmt.data.remote.FakeAccesoRemoteDataSource
import com.uade.tmt.data.remote.FakeInscripcionRemoteDataSource
import com.uade.tmt.data.remote.InscripcionRemoteDataSource
import com.uade.tmt.data.remote.InscripcionService
import com.uade.tmt.data.remote.SitioAccesoRemoteDataSource
import com.uade.tmt.data.remote.SitioInscripcionRemoteDataSource
import com.uade.tmt.data.remote.SitioRemoteDataSource
import com.uade.tmt.data.remote.SitioService
import com.uade.tmt.data.remote.TmtRemoteDataSource
import com.uade.tmt.data.repository.ClubRepositoryImpl
import com.uade.tmt.data.repository.JugadorRepositoryImpl
import com.uade.tmt.data.repository.ReservaRepositoryImpl
import com.uade.tmt.data.repository.SesionRepositoryImpl
import com.uade.tmt.data.repository.Sincronizador
import com.uade.tmt.data.repository.TorneoRepositoryImpl
import com.uade.tmt.domain.repository.ClubRepository
import com.uade.tmt.domain.repository.Conectividad
import com.uade.tmt.domain.repository.JugadorRepository
import com.uade.tmt.domain.repository.ReservaRepository
import com.uade.tmt.domain.repository.SesionRepository
import com.uade.tmt.domain.repository.TorneoRepository
import okhttp3.Interceptor
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
    val torneoRepository: TorneoRepository
    val jugadorRepository: JugadorRepository
    val clubRepository: ClubRepository
    val sesionRepository: SesionRepository
    val reservaRepository: ReservaRepository
    val conectividad: Conectividad
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

    /** Cookies de sesión del sitio, compartidas por el cliente HTTP y el acceso de jugadores. */
    private val cookies = CookieJarEnMemoria()

    /**
     * Cliente HTTP compartido por toda la app.
     *
     * - Timeouts de 15 s para conexión, lectura y escritura (RNF03), salvo la lectura del ranking.
     * - Sin HttpLoggingInterceptor: la app maneja credenciales y no deben quedar
     *   escritas en los logs (RNF06).
     * - Cookies solo en memoria: mantienen la sesión del jugador (RF02) sin escribirla en disco.
     */
    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .cookieJar(cookies)
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .addInterceptor(esperarMasElRanking)
            .build()
    }

    /**
     * Excepción a RNF03: el sitio llega a tardar más de 20 s en armar el ranking de una división.
     * Se carga de fondo y mientras tanto se ve lo guardado, así que esperar no frena ninguna pantalla.
     */
    private val esperarMasElRanking = Interceptor { chain ->
        val esRanking = chain.request().url.encodedPath.endsWith("/ranking.asp")
        val cadena = if (esRanking) chain.withReadTimeout(TIMEOUT_RANKING_SECONDS, TimeUnit.SECONDS) else chain
        cadena.proceed(chain.request())
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

    private val database: AppDatabase by lazy { AppDatabase.crear(context) }

    private val sincronizador: Sincronizador by lazy { Sincronizador(database.syncInfoDao()) }

    private val remoto: TmtRemoteDataSource by lazy {
        SitioRemoteDataSource(retrofit.create(SitioService::class.java))
    }

    override val torneoRepository: TorneoRepository by lazy {
        TorneoRepositoryImpl(database.torneoDao(), database.syncInfoDao(), remoto, sincronizador)
    }

    override val jugadorRepository: JugadorRepository by lazy {
        JugadorRepositoryImpl(database.jugadorDao(), database.syncInfoDao(), remoto, sincronizador)
    }

    override val clubRepository: ClubRepository by lazy {
        ClubRepositoryImpl(database.clubDao(), database.syncInfoDao(), remoto, sincronizador)
    }

    /** RF02: el sitio real solo con `BuildConfig.SITIO_REAL`; si no, el acceso de demostración. */
    private val acceso: AccesoRemoteDataSource by lazy {
        if (BuildConfig.SITIO_REAL) {
            SitioAccesoRemoteDataSource(retrofit.create(AccesoService::class.java), cookies)
        } else {
            FakeAccesoRemoteDataSource()
        }
    }

    override val sesionRepository: SesionRepository by lazy { SesionRepositoryImpl(acceso) }

    /** RF02: igual que el acceso. La fuente falsa arma las opciones con los torneos guardados. */
    private val inscripcion: InscripcionRemoteDataSource by lazy {
        if (BuildConfig.SITIO_REAL) {
            SitioInscripcionRemoteDataSource(retrofit.create(InscripcionService::class.java))
        } else {
            FakeInscripcionRemoteDataSource(torneoRepository)
        }
    }

    override val reservaRepository: ReservaRepository by lazy {
        ReservaRepositoryImpl(inscripcion, database.inscripcionDao(), conectividad)
    }

    override val conectividad: Conectividad by lazy { ConectividadAndroid(context) }

    private companion object {
        const val BASE_URL = "https://tenisdemesaparatodos.com/"
        const val TIMEOUT_SECONDS = 15L
        const val TIMEOUT_RANKING_SECONDS = 45
    }
}
