package fr.berliat.hskwidget.ui.screens.config.backupCloud

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import fr.berliat.googledrivebackup.BackupEvent
import fr.berliat.googledrivebackup.GoogleDriveBackupFile
import fr.berliat.googledrivebackup.GoogleDriveState
import fr.berliat.googledrivebackup.RestoreEvent
import fr.berliat.hskwidget.Utils
import fr.berliat.hskwidget.data.store.AppPreferencesStore
import fr.berliat.hskwidget.domain.DatabaseHelper
import fr.berliat.hskwidget.data.store.GoogleDriveBackup
import fr.berliat.hskwidget.ui.screens.config.backupCloud.BackupCloudTransferEvent.*

import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.cacheDir
import io.github.vinceglb.filekit.path
import io.github.vinceglb.filekit.size

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import java.io.FileInputStream
import java.io.FileOutputStream

actual class BackupCloudViewModel actual constructor(
    appConfig: AppPreferencesStore, val gDriveBackup: GoogleDriveBackup) :
    ViewModel() {
    actual val cloudLastBackup = appConfig.dbBackupCloudLastSuccess.asStateFlow()
    actual val isBusy = gDriveBackup.state.map { it == GoogleDriveState.Busy }
        .stateIn(
            scope = CoroutineScope(Dispatchers.Default),
            started = SharingStarted.Lazily,
            initialValue = false
        )
    val _transferState = MutableStateFlow<BackupCloudTransferEvent?>(value = null)
    actual val transferState: StateFlow<BackupCloudTransferEvent?> = _transferState

    actual fun backup() {
        gDriveBackup.login {
            viewModelScope.launch {
                val gDriveBackupSnapshot = DatabaseHelper.getInstance().snapshotDatabase()
                val flow = gDriveBackup.backup(
                    listOf(
                        GoogleDriveBackupFile.UploadFile(
                            "database.sqlite",
                            FileInputStream(gDriveBackupSnapshot.path),
                            "application/octet-stream",
                            gDriveBackupSnapshot.size()
                        )
                    ),
                    onlyKeepMostRecent = true
                )

                flow.takeUntilInclusive { event ->
                    !(event is BackupEvent.Success || event is BackupEvent.Failed || event is BackupEvent.Cancelled)
                }.collect { event ->
                    when (event) {
                        is BackupEvent.Cancelled -> _transferState.emit(Cancelled)
                        is BackupEvent.Failed -> _transferState.emit(Failed(event.exception))
                        is BackupEvent.Progress -> _transferState.emit(
                            Progress(
                                fileIndex = event.fileIndex,
                                fileCount = event.fileCount,
                                bytesReceived = event.bytesSent,
                                bytesTotal = event.bytesTotal
                            )
                        )

                        is BackupEvent.Started -> _transferState.emit(Started)
                        is BackupEvent.Success -> _transferState.emit(Success)
                    }
                }
            }
        }
    }

    actual fun restore() {
        gDriveBackup.login {
            viewModelScope.launch {
                val destination =
                    PlatformFile(FileKit.cacheDir.path + "/" + Utils.getRandomString(10))

                val flow = gDriveBackup.restore(
                    listOf(
                        GoogleDriveBackupFile.DownloadFile(
                            "database.sqlite",
                            FileOutputStream(destination.path)
                        )
                    )
                )

                flow.takeUntilInclusive { event ->
                    !(event is RestoreEvent.Success || event is RestoreEvent.Failed || event is RestoreEvent.Cancelled)
                }.collect { event ->
                    when (event) {
                        is RestoreEvent.Cancelled -> _transferState.emit(Cancelled)
                        is RestoreEvent.Failed -> _transferState.emit(Failed(event.exception))
                        is RestoreEvent.Progress -> _transferState.emit(
                            Progress(
                                fileIndex = event.fileIndex,
                                fileCount = event.fileCount,
                                bytesReceived = event.bytesReceived,
                                bytesTotal = event.bytesTotal
                            )
                        )

                        is RestoreEvent.Started -> _transferState.emit(Started)
                        is RestoreEvent.Success -> _transferState.emit(Success)
                        is RestoreEvent.Empty -> TODO()
                    }
                }
            }
        }
    }

    actual fun cancel() = gDriveBackup.cancel()
}

fun <T> Flow<T>.takeUntilInclusive(predicate: (T) -> Boolean): Flow<T> = flow {
    collect { value ->
        emit(value)
        if (predicate(value)) return@collect // exit collection but keep coroutine alive
    }
}
