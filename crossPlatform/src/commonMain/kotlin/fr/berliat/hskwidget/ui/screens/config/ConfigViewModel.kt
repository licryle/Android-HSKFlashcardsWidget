package fr.berliat.hskwidget.ui.screens.config

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.berliat.hskwidget.AnkiDelegator

import fr.berliat.hskwidget.core.HSKAppServices
import fr.berliat.hskwidget.data.store.AppPreferencesStore
import fr.berliat.hskwidget.domain.DatabaseDiskBackup
import fr.berliat.hskwidget.domain.DatabaseHelper
import fr.berliat.hskwidget.ui.screens.config.ankiSync.AnkiSyncViewModel
import fr.berliat.hskwidget.ui.screens.config.backupCloud.BackupCloudViewModel
import fr.berliat.hskwidget.ui.screens.config.backupDisk.BackupDiskViewModel

import okio.Path

import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okio.FileSystem

class ConfigViewModel(
    val appConfig: AppPreferencesStore = HSKAppServices.appPreferences,
    val ankiDelegate: AnkiDelegator
): ViewModel() {
    val backupDiskViewModel = BackupDiskViewModel()
    val backupCloudViewModel = BackupCloudViewModel(appConfig)
    val ankiSyncViewModel = AnkiSyncViewModel()
}
