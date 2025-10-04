package fr.berliat.hskwidget.ui.screens.config.ankiSync

import fr.berliat.ankidroidhelper.AnkiDelegate
import fr.berliat.hskwidget.KAnkiServiceDelegator
import fr.berliat.hskwidget.core.HSKAppServices
import fr.berliat.hskwidget.data.store.AppPreferencesStore
import fr.berliat.hskwidget.ui.screens.config.ConfigViewModel

import kotlinx.coroutines.flow.StateFlow

enum class SyncState {
    NOT_STARTED,
    STARTING,
    IN_PROGRESS,
    SUCCESS,
    CANCELLED,
    FAILED
}

class SyncProgress(val state: SyncState, val current: Int, val total: Int, val message: String)

expect class AnkiSyncViewModel(
    ankiDelegate: AnkiDelegate = HSKAppServices.ankiDelegate,
    appConfig: AppPreferencesStore = HSKAppServices.appPreferences
) {
    val isAvailableOnThisPlatform: Boolean
    val ankiActive : StateFlow<Boolean>
    val syncProgress : StateFlow<SyncProgress>

    fun toggleAnkiActive(enabled: Boolean)

    fun cancelSync()
}