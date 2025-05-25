package fr.berliat.hskwidget

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class HSKHelperApp : Application() {
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
}