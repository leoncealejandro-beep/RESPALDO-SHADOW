package com.example

import android.app.Application
import com.example.ui.apps.AppRegistry

class ShadowVRApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Registra todas las apps nativas UNA SOLA VEZ al arrancar
        AppRegistry.registerAll()
    }
}