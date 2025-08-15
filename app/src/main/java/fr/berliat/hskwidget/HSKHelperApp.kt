package fr.berliat.hskwidget

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class HSKHelperApp : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        lateinit var instance: HSKHelperApp
            private set
    }
}