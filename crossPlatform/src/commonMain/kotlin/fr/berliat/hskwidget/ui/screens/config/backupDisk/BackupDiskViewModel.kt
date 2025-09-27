package fr.berliat.hskwidget.ui.screens.config.backupDisk

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import fr.berliat.hskwidget.Utils
import fr.berliat.hskwidget.core.HSKAppServices
import fr.berliat.hskwidget.data.store.AppPreferencesStore
import fr.berliat.hskwidget.domain.DatabaseDiskBackup
import fr.berliat.hskwidget.domain.DatabaseHelper

import hskflashcardswidget.crossplatform.generated.resources.Res
import hskflashcardswidget.crossplatform.generated.resources.config_backup_directory_failed_selection
import hskflashcardswidget.crossplatform.generated.resources.dbrestore_failure_nofileselected
import hskflashcardswidget.crossplatform.generated.resources.dbrestore_success

import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.bookmarkData
import io.github.vinceglb.filekit.cacheDir
import io.github.vinceglb.filekit.copyTo
import io.github.vinceglb.filekit.delete
import io.github.vinceglb.filekit.div
import io.github.vinceglb.filekit.name

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import org.jetbrains.compose.resources.getString


class BackupDiskViewModel(
    val appConfig: AppPreferencesStore = HSKAppServices.appPreferences): ViewModel() {
    val backupDiskActive = appConfig.dbBackUpDiskActive.asStateFlow()
    val backupDiskMaxFiles = appConfig.dbBackUpDiskMaxFiles.asStateFlow()

    private val databaseDiskBackup = DatabaseDiskBackup(appConfig)
    val backupDiskFolder: StateFlow<PlatformFile?> = appConfig.dbBackUpDiskDirectory
        .asStateFlow()
        .map { path -> databaseDiskBackup.getPlatformFileFromBookmarkOrNull(path) } // Ensuring the view only sees an accessible directory
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = databaseDiskBackup.getPlatformFileFromBookmarkOrNull(appConfig.dbBackUpDiskDirectory.value)
        )

    init {
        if (backupDiskFolder.value == null) {
            appConfig.dbBackUpDiskActive.value = false
        }
    }

    fun toggleBackupDiskActive(active: Boolean) {
        if (!active) {
            appConfig.dbBackUpDiskActive.value = false
            return
        }

        if (backupDiskFolder.value == null) {
            selectBackupFolder()
        } else {
            appConfig.dbBackUpDiskActive.value = true
        }
    }

    fun setBackupDiskMaxFiles(value: Int) {
        appConfig.dbBackUpDiskMaxFiles.value = value
    }

    fun selectRestoreFile() {
        viewModelScope.launch(Dispatchers.Main) {
            databaseDiskBackup.selectBackupFile(
                onSuccess = { file ->
                    viewModelScope.launch(Dispatchers.IO) {
                        val dbHelper = DatabaseHelper.getInstance()
                        val copiedFile = FileKit.cacheDir / file.name
                        file.copyTo(FileKit.cacheDir / file.name)
                        // TODO handle copy fail?
                        val sourceDb = dbHelper.loadExternalDatabase(copiedFile)
                        dbHelper.replaceUserDataInDB(dbHelper.liveDatabase, sourceDb)
                        copiedFile.delete()

                        withContext(Dispatchers.Main) {
                            Utils.toast(Res.string.dbrestore_success)

                            // TODO reconnect navigation
                            /*val action = ConfigFragmentDirections.search()
                            findNavController().navigate(action)*/
                        }

                        // Backup was successful, let's trigger widget updates, hoping any matches
                        //WidgetController.().updateAllFlashCardWidgets()


                        Utils.logAnalyticsEvent(Utils.ANALYTICS_EVENTS.CONFIG_BACKUP_RESTORE)
                    }
                },
                onFail = { e ->
                    viewModelScope.launch(Dispatchers.Main) {
                        Utils.toast(Res.string.dbrestore_failure_nofileselected)

                        Utils.logAnalyticsError(
                            "BACKUP_RESTORE",
                            getString(Res.string.dbrestore_failure_nofileselected),
                            e.message ?: ""
                        )
                    }
                }
            )
        }
    }

    fun selectBackupFolder() {
        viewModelScope.launch(Dispatchers.Main) {
            databaseDiskBackup.selectFolder(
                onSuccess = { folder ->
                    // persist permissions in Platform && DataStore
                    viewModelScope.launch { appConfig.dbBackUpDiskDirectory.value = folder.bookmarkData() }
                    appConfig.dbBackUpDiskActive.value = true
                    Utils.logAnalyticsEvent(Utils.ANALYTICS_EVENTS.CONFIG_BACKUP_ON)
                },
                onFail = {
                    Utils.toast(Res.string.config_backup_directory_failed_selection)
                }
            )
        }
    }
}