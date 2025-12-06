package fr.berliat.hskwidget.data.store

import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

actual sealed class GoogleDriveState
actual class UploadFile
actual sealed class BackupEvent
actual class DownloadFile
actual sealed class RestoreEvent
actual class File
actual class Account
actual class GoogleDriveBackup {
    actual val state: StateFlow<GoogleDriveState>
        get() = TODO("Not yet implemented")

    actual var transferChunkSize: Int
        get() = TODO("Not yet implemented")
        set(value) { TODO("Not yet implemented") }

    actual fun cancel() {
    }

    actual fun login(onlyFromCache: Boolean, successCallback: (() -> Unit)?) {
    }

    actual fun logout(
        account: Account,
        successCallback: (() -> Unit)?
    ) {
    }

    actual fun backup(
        files: List<UploadFile>,
        onlyKeepMostRecent: Boolean
    ): SharedFlow<BackupEvent> {
        TODO("Not yet implemented")
    }

    actual fun restore(
        filesWanted: List<DownloadFile>,
        onlyMostRecent: Boolean
    ): SharedFlow<RestoreEvent> {
        TODO("Not yet implemented")
    }

    actual suspend fun listBackedUpFiles(): List<File> {
        TODO("Not yet implemented")
    }

    actual suspend fun deletePreviousBackups(): Result<Unit> {
        TODO("Not yet implemented")
    }
}