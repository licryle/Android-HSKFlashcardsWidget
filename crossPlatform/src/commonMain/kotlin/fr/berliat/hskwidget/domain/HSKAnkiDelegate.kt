package fr.berliat.hskwidget.domain

import kotlin.reflect.KClass

typealias KAnkiDelegator = suspend ((suspend () -> Result<Unit>)?) -> Unit?
typealias KAnkiServiceDelegator = suspend (serviceClass: KClass<out AnkiSyncWordListsService>) -> Unit?

expect class HSKAnkiDelegate {
    suspend fun modifyAnki(ankiAction: (suspend () -> Result<Unit>)?)
    suspend fun modifyAnkiViaService(serviceClass: KClass<out AnkiSyncWordListsService>)
}