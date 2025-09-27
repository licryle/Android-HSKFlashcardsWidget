package fr.berliat.hskwidget.ui.screens.config.backupCloud

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.berliat.hskwidget.core.HSKAppServices
import fr.berliat.hskwidget.data.store.AppPreferencesStore
import fr.berliat.hskwidget.domain.DatabaseHelper
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okio.FileSystem
import okio.Path

class BackupCloudViewModel(
    val appConfig: AppPreferencesStore = HSKAppServices.appPreferences): ViewModel() {

/*
    val cloudRestoreFilePath
        get() = File("${requireContext().cacheDir}/restore/${fr.berliat.hskwidget.data.store.DatabaseHelper.DATABASE_FILENAME}")

    val cloudLastBackup = appConfig.dbBackupCloudLastSuccess.asStateFlow()

    private val gDriveBackup = GoogleDriveBackup(null, null, "HSKWidget") // supply Activity reference if needed

    private var gDriveBackupSnapshot: Path? = null
    private val gDriveBackupMutex = Mutex()

    fun backupToCloud() {
        viewModelScope.launch {
            val dbHelper = DatabaseHelper.getInstance(context)
            gDriveBackupMutex.withLock {
                gDriveBackupSnapshot = dbHelper.snapshotDatabase()
                gDriveBackup.backup(
                    listOf(
                        GoogleDriveBackupFile.UploadFile(
                            "database.sqlite",
                            FileInputStream(gDriveBackupSnapshot),
                            "application/octet-stream",
                            gDriveBackupSnapshot!!.length()
                        )
                    )
                )
            }
        }
    }

    fun restoreFromCloud(destination: Path) {
        viewModelScope.launch {
            val fs: FileSystem = FileSystem

            destination.parent?.mkdirs()
            gDriveBackup.restore(
                listOf(
                    GoogleDriveBackupFile.DownloadFile(
                        "database.sqlite",
                        FileOutputStream(destination)
                    )
                )
            )
        }
    }*/
}