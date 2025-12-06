package fr.berliat.hskwidget.data.store

import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

expect sealed class GoogleDriveState
expect class UploadFile
expect sealed class BackupEvent
expect class DownloadFile
expect sealed class RestoreEvent
expect class File
expect class Account

expect class GoogleDriveBackup {
    val state: StateFlow<GoogleDriveState>
    var transferChunkSize: Int

    fun cancel()

    fun login(onlyFromCache: Boolean = false, successCallback: (() -> Unit)? = null)

    fun logout(account: Account, successCallback: (() -> Unit)? = null)

    fun backup(
        files: List<UploadFile>,
        onlyKeepMostRecent: Boolean = true
    ): SharedFlow<BackupEvent>

    fun restore(
        filesWanted: List<DownloadFile>,
        onlyMostRecent: Boolean = true
    ): SharedFlow<RestoreEvent>

    suspend fun listBackedUpFiles(): List<File>

    suspend fun deletePreviousBackups(): Result<Unit>
}