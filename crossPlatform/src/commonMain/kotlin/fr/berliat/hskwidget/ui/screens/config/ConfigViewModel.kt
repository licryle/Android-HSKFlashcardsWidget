package fr.berliat.hskwidget.ui.screens.config

import androidx.lifecycle.ViewModel

import fr.berliat.googledrivebackup.GoogleDriveBackup

import fr.berliat.hskwidget.core.HSKAppServices
import fr.berliat.hskwidget.data.store.AppPreferencesStore
import fr.berliat.hskwidget.domain.HSKAnkiDelegate
import fr.berliat.hskwidget.ui.screens.config.ankiSync.AnkiSyncViewModel
import fr.berliat.hskwidget.ui.screens.config.backupCloud.BackupCloudViewModel
import fr.berliat.hskwidget.ui.screens.config.backupDisk.BackupDiskViewModel
import fr.berliat.hskwidget.ui.widget.FlashcardWidgetProvider

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

    val backupDiskViewModel = BackupDiskViewModel()
    val backupCloudViewModel = BackupCloudViewModel(appConfig, gDriveBackup)
    val ankiSyncViewModel = AnkiSyncViewModel(ankiDelegate)
}
