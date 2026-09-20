package fr.berliat.hskwidget.ui.screens.config

import androidx.lifecycle.ViewModel

import fr.berliat.googledrivebackup.GoogleDriveBackup

import fr.berliat.hskwidget.core.HSKAppServices
import fr.berliat.hskwidget.core.SnackbarType
import fr.berliat.hskwidget.data.store.AppPreferencesStore
import fr.berliat.hskwidget.domain.DatabaseHelper
import fr.berliat.hskwidget.domain.HSKAnkiDelegate
import fr.berliat.hskwidget.ui.screens.config.ankiSync.AnkiSyncViewModel
import fr.berliat.hskwidget.ui.screens.config.backupCloud.BackupCloudViewModel
import fr.berliat.hskwidget.ui.screens.config.backupDisk.BackupDiskViewModel
import fr.berliat.hskwidget.ui.widget.FlashcardWidgetProvider
import fr.berliat.hskwidget.Res
import fr.berliat.hskwidget.database_update_success
import fr.berliat.hskwidget.database_update_failure
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ConfigViewModel(
    private val appConfig: AppPreferencesStore = HSKAppServices.appPreferences,
    private val widgetProvider: FlashcardWidgetProvider = FlashcardWidgetProvider(),
    ankiDelegate: HSKAnkiDelegate = HSKAppServices.ankiDelegate,
    gDriveBackup: GoogleDriveBackup
): ViewModel() {
    fun onLanguageChange() {
        if (appConfig.dictionaryLocale.value == null) {
            widgetProvider.redrawAllFlashCardWidgets()
        }
    }

    fun repairDatabase() {
        CoroutineScope(Dispatchers.Main).launch {
            DatabaseHelper.getInstance().updateLiveDatabaseFromAsset({
                HSKAppServices.snackbar.show(SnackbarType.SUCCESS, Res.string.database_update_success)
            }, { e ->
                HSKAppServices.snackbar.show(SnackbarType.ERROR, Res.string.database_update_failure, listOf(e.message ?: ""))
            }, force = true)
        }
    }

    val backupDiskViewModel = BackupDiskViewModel()
    val backupCloudViewModel = BackupCloudViewModel(appConfig, gDriveBackup)
    val ankiSyncViewModel = AnkiSyncViewModel(ankiDelegate)
}
