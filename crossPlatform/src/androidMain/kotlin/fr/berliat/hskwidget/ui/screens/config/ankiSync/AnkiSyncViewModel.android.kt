package fr.berliat.hskwidget.ui.screens.config.ankiSync

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import co.touchlab.kermit.Logger

import fr.berliat.ankidroidhelper.AnkiDelegate
import fr.berliat.ankidroidhelper.AnkiSyncServiceDelegate
import fr.berliat.hskwidget.ExpectedUtils.requestPermissionNotification
import fr.berliat.hskwidget.KAnkiServiceDelegator
import fr.berliat.hskwidget.Utils
import fr.berliat.hskwidget.YYMMDDHHMMSS
import fr.berliat.hskwidget.core.HSKAppServices
import fr.berliat.hskwidget.data.store.AppPreferencesStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

actual class AnkiSyncViewModel actual constructor(
    val ankiDelegate: KAnkiServiceDelegator,
    val appConfig: AppPreferencesStore)
    : ViewModel(), AnkiDelegate.HandlerInterface {
    actual val isAvailableOnThisPlatform = true

    actual val ankiActive = appConfig.ankiSaveNotes.asStateFlow()

    private val _syncProgress = MutableStateFlow(SyncProgress(SyncState.NOT_STARTED, 0, 0, ""))
    actual val syncProgress: StateFlow<SyncProgress> = _syncProgress.asStateFlow()

    var ankiSyncServiceDelegate: AnkiSyncServiceDelegate? = null

    override fun onAnkiOperationSuccess() {
        Logger.i(tag = TAG, messageString = "onAnkiOperationSuccess: completed full import into Anki")

        _syncProgress.value = SyncProgress(SyncState.SUCCESS, 0, 0,
            Clock.System.now().YYMMDDHHMMSS())
    }

    override fun onAnkiOperationCancelled() {
        Logger.i(tag = TAG, messageString = "onAnkiOperationCancelled: full Anki import cancelled by user")

        _syncProgress.value = SyncProgress(SyncState.CANCELLED, 0, 0, "")
    }

    override fun onAnkiOperationFailed(e: Throwable) {
        Logger.i(tag = TAG, messageString = "onAnkiOperationFailed: failed full import into Anki")

        _syncProgress.value = SyncProgress(SyncState.FAILED, 0, 0, e.message ?: "")

        Utils.logAnalyticsError(
            "ANKI_SYNC",
            "FullAnkiImportFailed",
            e.message ?: ""
        )
    }

    override fun onAnkiSyncProgress(
        current: Int,
        total: Int,
        message: String
    ) {
        Logger.i(tag = TAG, messageString = "onAnkiImportProgress: $current / $total")

        _syncProgress.value = SyncProgress(
            SyncState.IN_PROGRESS,
            current,
            total,
            message
        )
    }

    override fun onAnkiRequestPermissionGranted() {
        appConfig.ankiSaveNotes.value = true
    }

    override fun onAnkiRequestPermissionDenied() {
        appConfig.ankiSaveNotes.value = false

        _syncProgress.value = SyncProgress(SyncState.NOT_STARTED, 0, 0, "")
    }

    override fun onAnkiServiceStarting(serviceDelegate: AnkiSyncServiceDelegate) {
        ankiSyncServiceDelegate = serviceDelegate

        requestPermissionNotification()

        _syncProgress.value = SyncProgress(SyncState.STARTING, 0, 0, "")
    }

    actual fun toggleAnkiActive(enabled: Boolean) {
        appConfig.ankiSaveNotes.value = enabled

        if (enabled) {
            importsAllNotesToAnkiDroid()
            Utils.logAnalyticsEvent(Utils.ANALYTICS_EVENTS.CONFIG_ANKI_SYNC_ON)
        } else {
            cancelSync()
            Utils.logAnalyticsEvent(Utils.ANALYTICS_EVENTS.CONFIG_ANKI_SYNC_OFF)
        }
    }

    private fun importsAllNotesToAnkiDroid() {
        viewModelScope.launch {
            ankiDelegate.invoke(HSKAppServices.wordListRepo.syncListsToAnki())
        }
    }

    actual fun cancelSync() {
        ankiSyncServiceDelegate?.cancelCurrentOperation()
    }

    companion object {
        private const val TAG = "AnkiSyncViewModel"
    }
}
