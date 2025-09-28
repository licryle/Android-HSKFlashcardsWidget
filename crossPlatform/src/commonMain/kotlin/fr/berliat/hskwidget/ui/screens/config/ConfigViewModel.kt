package fr.berliat.hskwidget.ui.screens.config

import androidx.lifecycle.ViewModel

import fr.berliat.hskwidget.KAnkiServiceDelegator
import fr.berliat.hskwidget.core.HSKAppServices
import fr.berliat.hskwidget.data.store.AppPreferencesStore
import fr.berliat.hskwidget.ui.screens.config.ankiSync.AnkiSyncViewModel
import fr.berliat.hskwidget.ui.screens.config.backupCloud.BackupCloudViewModel
import fr.berliat.hskwidget.ui.screens.config.backupDisk.BackupDiskViewModel

class ConfigViewModel(
    val appConfig: AppPreferencesStore = HSKAppServices.appPreferences,
    val ankiDelegate: KAnkiServiceDelegator
): ViewModel() {
    val backupDiskViewModel = BackupDiskViewModel()
    val backupCloudViewModel = BackupCloudViewModel(appConfig)
    val ankiSyncViewModel = AnkiSyncViewModel(ankiDelegate)
}
