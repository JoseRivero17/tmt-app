package com.uade.tmt

import android.app.Application
import com.uade.tmt.di.AppContainer
import com.uade.tmt.di.DefaultAppContainer

/**
 * Clase Application de TMT. Crea el contenedor de dependencias una sola vez por proceso.
 */
class TMTApplication : Application() {

    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)
    }
}
