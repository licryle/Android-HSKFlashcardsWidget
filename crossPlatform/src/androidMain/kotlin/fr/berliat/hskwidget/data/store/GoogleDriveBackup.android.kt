package fr.berliat.hskwidget.data.store

import androidx.fragment.app.FragmentActivity
import fr.berliat.googledrivebackup.GoogleDriveBackup
import kotlinx.coroutines.flow.StateFlow

actual typealias BackupEvent = fr.berliat.googledrivebackup.BackupEvent
actual typealias RestoreEvent = fr.berliat.googledrivebackup.RestoreEvent

actual typealias GoogleDriveState = fr.berliat.googledrivebackup.GoogleDriveState

actual typealias DownloadFile = fr.berliat.googledrivebackup.GoogleDriveBackupFile.DownloadFile
actual typealias UploadFile = fr.berliat.googledrivebackup.GoogleDriveBackupFile.UploadFile

actual typealias File = com.google.api.services.drive.model.File
actual typealias Account = android.accounts.Account

actual class GoogleDriveBackup(val activity: FragmentActivity, val appName: String) {
    private val gDriveBackup = GoogleDriveBackup(
        activity = activity,
        appName = appName
    )

    actual val state: StateFlow<GoogleDriveState> get() = gDriveBackup.state
    actual var transferChunkSize: Int
        get() = gDriveBackup.transferChunkSize
        set(value) { gDriveBackup.transferChunkSize = value }

    actual fun cancel() = gDriveBackup.cancel()
    actual fun login(onlyFromCache: Boolean, successCallback: (() -> Unit)?) = gDriveBackup.login(onlyFromCache, successCallback)
    actual fun logout(account: Account, successCallback: (() -> Unit)?) = gDriveBackup.logout(account, successCallback)
    actual fun backup(files: List<UploadFile>, onlyKeepMostRecent: Boolean) = gDriveBackup.backup(files, onlyKeepMostRecent)
    actual fun restore(filesWanted: List<DownloadFile>, onlyMostRecent: Boolean) = gDriveBackup.restore(filesWanted, onlyMostRecent)
    actual suspend fun listBackedUpFiles() = gDriveBackup.listBackedUpFiles()
    actual suspend fun deletePreviousBackups() = gDriveBackup.deletePreviousBackups()
}