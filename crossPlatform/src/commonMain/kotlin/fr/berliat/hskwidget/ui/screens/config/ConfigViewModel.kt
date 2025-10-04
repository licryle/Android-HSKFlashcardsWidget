package fr.berliat.hskwidget.ui.screens.config

import androidx.lifecycle.ViewModel

import fr.berliat.ankidroidhelper.AnkiDelegate
import fr.berliat.hskwidget.core.HSKAppServices
import fr.berliat.hskwidget.data.store.AppPreferencesStore
import fr.berliat.hskwidget.data.store.GoogleDriveBackup
import fr.berliat.hskwidget.ui.screens.config.ankiSync.AnkiSyncViewModel
import fr.berliat.hskwidget.ui.screens.config.backupCloud.BackupCloudViewModel
import fr.berliat.hskwidget.ui.screens.config.backupDisk.BackupDiskViewModel

class ConfigViewModel(
    appConfig: AppPreferencesStore = HSKAppServices.appPreferences,
    ankiDelegate: AnkiDelegate = HSKAppServices.ankiDelegate,
    gDriveBackup: GoogleDriveBackup
): ViewModel() {
    val backupDiskViewModel = BackupDiskViewModel()
    val backupCloudViewModel = BackupCloudViewModel(appConfig, gDriveBackup)
    val ankiSyncViewModel = AnkiSyncViewModel(ankiDelegate)
}
