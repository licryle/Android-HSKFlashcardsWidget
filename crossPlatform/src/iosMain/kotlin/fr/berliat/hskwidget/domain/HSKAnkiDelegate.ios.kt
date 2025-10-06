package fr.berliat.hskwidget.domain

import kotlin.reflect.KClass

actual class HSKAnkiDelegate {
    actual suspend fun modifyAnki(ankiAction: (suspend () -> Result<Unit>)?) { }
    actual suspend fun modifyAnkiViaService(serviceClass: KClass<out AnkiSyncWordListsService>) { }
}