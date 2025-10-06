package fr.berliat.hskwidget.ui.screens.config.ankiSync

import fr.berliat.hskwidget.data.store.AppPreferencesStore
import fr.berliat.hskwidget.domain.HSKAnkiDelegate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

actual class AnkiSyncViewModel actual constructor(
    ankiDelegate: HSKAnkiDelegate,
    appConfig: AppPreferencesStore
) {
    actual val isAvailableOnThisPlatform: Boolean
        get() = false
    actual val ankiActive: StateFlow<Boolean>
        get() = MutableStateFlow(false)
    actual val syncProgress: StateFlow<SyncProgress>
        get() = MutableStateFlow(SyncProgress(
            state = SyncState.NOT_STARTED,
            current =0,
            total = 0,
            message = ""
        ))

    actual fun toggleAnkiActive(enabled: Boolean) {
    }

    actual fun cancelSync() {
    }
}