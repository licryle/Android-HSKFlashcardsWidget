package fr.berliat.hskwidget.ui.application

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import fr.berliat.hskwidget.core.Utils
import fr.berliat.hskwidget.core.AppServices
import fr.berliat.hskwidget.core.HSKAppServices
import fr.berliat.hskwidget.data.store.AppPreferencesStore
import fr.berliat.hskwidget.domain.DatabaseDiskBackup
import fr.berliat.hskwidget.domain.DatabaseHelper
import fr.berliat.hskwidget.ui.navigation.Screen
import fr.berliat.hskwidget.Res
import fr.berliat.hskwidget.core.AppDispatchers
import fr.berliat.hskwidget.core.HSKAppServicesPriority
import fr.berliat.hskwidget.core.Logging
import fr.berliat.hskwidget.database_update_failure
import fr.berliat.hskwidget.database_update_start
import fr.berliat.hskwidget.database_update_success
import fr.berliat.hskwidget.dbbackup_failure_folderpermission
import fr.berliat.hskwidget.dbbackup_failure_write
import fr.berliat.hskwidget.dbbackup_success
import fr.berliat.hskwidget.ui.navigation.NavigationManager

import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.fromBookmarkData
import io.github.vinceglb.filekit.path

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch

expect class AppViewModel: CommonAppViewModel

open class CommonAppViewModel(val navigationManager: NavigationManager): ViewModel() {
    var _appConfig: AppPreferencesStore? = null
    val appConfig
        get() = _appConfig!!

    private val _isReady = MutableStateFlow<Boolean>(false)
    val isReady = _isReady.asSharedFlow()

    // Queue for actions that need to be executed after initialization
    private val pendingActions = mutableListOf<() -> Unit>()
    private var isInitialized = false

    open fun init() {
        HSKAppServices.init(HSKAppServicesPriority.PartialApp)
        // Launch a coroutine that reacts to changes
        viewModelScope.launch(AppDispatchers.IO) {
            HSKAppServices.status
                .filter  { status -> status is AppServices.Status.Ready }
                .take(1)
                .collect{ status ->
                    val readyStatus = status as AppServices.Status.Ready
                    if (readyStatus.upToPrio >= HSKAppServicesPriority.FullApp) {
                        // Already ready from a previous launch still in memory
                        _isReady.value = true
                        executePendingActions()
                    } else {
                        finishInitialization()
                    }
                }
        }
    }

    protected open suspend fun finishInitialization() {
        _appConfig = HSKAppServices.appPreferences
        _isReady.value = true

        handleDbOperations()

        if (didUpdateApp()) handleAppUpdate()

        executePendingActions()
    }

    protected open suspend fun executePendingActions() {
        // Mark as initialized and process any pending actions
        synchronized(pendingActions) {
            isInitialized = true
            pendingActions.forEach { it.invoke() }
            pendingActions.clear()
        }
    }

    protected open fun handleAppUpdate() {
        if (shouldUpdateDatabaseFromAsset()) {
            Utils.toast(Res.string.database_update_start)

            viewModelScope.launch(AppDispatchers.IO) {
                DatabaseHelper.getInstance().updateLiveDatabaseFromAsset({
                    Utils.toast(Res.string.database_update_success)
                }, { e ->
                    Utils.toast(Res.string.database_update_failure, listOf(e.message ?: ""))

                    Logging.logAnalyticsError(TAG, "UpdateDatabaseFromAssetFailure", e.message ?: "")
                })
            }
        }

        appConfig.appVersionCode.value = Utils.getAppVersion()
    }

    fun didUpdateApp(): Boolean {
        return appConfig.appVersionCode.value != Utils.getAppVersion()
    }

    private fun handleDbOperations() {
        viewModelScope.launch(AppDispatchers.IO) {
            DatabaseHelper.cleanTempDatabaseFiles()
        }

        handleBackupDisk()
    }

    fun shouldUpdateDatabaseFromAsset(): Boolean {
        if (appConfig.appVersionCode.value == 0) return false // first launch, nothing to update

        val updateDbVersions = listOf(32, 37)

        return updateDbVersions.any { updateVersion ->
            appConfig.appVersionCode.value < updateVersion && Utils.getAppVersion() >= updateVersion
        }
    }

    private fun handleBackupDisk() {
        val bookMark = appConfig.dbBackUpDiskDirectory.value
        if (appConfig.dbBackUpDiskActive.value && bookMark != null) {
            val backupFolder = PlatformFile.fromBookmarkData(bookMark)
            viewModelScope.launch(AppDispatchers.IO) {
                DatabaseDiskBackup.getFolder(
                    bookMark,
                    onSuccess = {
                        viewModelScope.launch(AppDispatchers.IO) {
                            DatabaseDiskBackup.backUp(
                                bookMark,
                                onSuccess = {
                                    Utils.toast(Res.string.dbbackup_success)
                                    viewModelScope.launch(AppDispatchers.IO) {
                                        DatabaseDiskBackup.cleanOldBackups(
                                            backupFolder,
                                            appConfig.dbBackUpDiskMaxFiles.value
                                        )
                                    }
                                },
                                onFail = { Utils.toast(Res.string.dbbackup_failure_write) }
                            )
                        }
                    },
                    onFail = {
                        Utils.toast(Res.string.dbbackup_failure_folderpermission)
                    }
                )
            }
        }
    }

    fun search(s: String) {
        navigationManager.navigate(Screen.Dictionary(s))
    }

    fun configureWidget(widgetId: Int) {
        navigationManager.navigate(Screen.Widgets(widgetId, true))
    }

    fun ocrImage(imageFile: PlatformFile) {
        navigationManager.navigate(Screen.OCRDisplay("", imageFile.path))
    }

    open fun finalizeWidgetConfiguration(widgetId: Int) { }

    /**
     * Execute an action either immediately (if initialized) or queue it for later execution.
     * This prevents race conditions when handling intents before services are ready.
     */
    protected fun executeWhenReady(action: () -> Unit) {
        synchronized(pendingActions) {
            if (isInitialized) {
                action.invoke()
            } else {
                pendingActions.add(action)
            }
        }
    }

    companion object {
        private const val TAG = "AppViewModel"
    }
}