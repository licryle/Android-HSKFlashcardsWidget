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

import hskflashcardswidget.crossplatform.generated.resources.Res
import hskflashcardswidget.crossplatform.generated.resources.dbrestore_failure_fileformat
import hskflashcardswidget.crossplatform.generated.resources.dbrestore_failure_import
import hskflashcardswidget.crossplatform.generated.resources.dbrestore_start
import hskflashcardswidget.crossplatform.generated.resources.dbrestore_success
import hskflashcardswidget.crossplatform.generated.resources.googledrive_restore_nofile

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
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.jetbrains.compose.resources.getString

import java.io.FileInputStream
import java.io.FileOutputStream

actual class BackupCloudViewModel actual constructor(
    val appConfig: AppPreferencesStore, val gDriveBackup: GoogleDriveBackup) :
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

    actual val restoreFileFrom = MutableStateFlow<Instant?>(null)

    private val cloudRestoreFile = PlatformFile(FileKit.cacheDir.path + "/" + Utils.getRandomString(10))

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
                        is BackupEvent.Success -> {
                            _transferState.emit(Success)
                            appConfig.dbBackupCloudLastSuccess.value = Clock.System.now()
                            gDriveBackup.deletePreviousBackups()
                        }
                    }
                }
            }
        }
    }

    actual fun restore() {
        gDriveBackup.login {
            viewModelScope.launch {
                val flow = gDriveBackup.restore(
                    listOf(
                        GoogleDriveBackupFile.DownloadFile(
                            "database.sqlite",
                            FileOutputStream(cloudRestoreFile.path)
                        )
                    )
                )

                flow.takeUntilInclusive { event ->
                    !(event is RestoreEvent.Success || event is RestoreEvent.Failed
                            || event is RestoreEvent.Cancelled || event is RestoreEvent.Empty)
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
                        is RestoreEvent.Success -> {
                            _transferState.emit(Success)

                            if (event.files.isEmpty() || event.files[0].name != "database.sqlite") {
                                throw Exception("ConfigFragement.onRestoreSuccess: Something went really wrong in GoogleDriveBackUp lib, wrong backup file")
                            }

                            restoreFileFrom.value = Instant.fromEpochSeconds(event.files[0].modifiedTime?.epochSecond ?: 0)
                        }
                        is RestoreEvent.Empty -> _transferState.emit(Failed(Exception("No Backup File")))
                    }
                }
            }
        }
    }

    actual fun confirmRestoration() {
        Utils.toast(Res.string.dbrestore_start)

        Utils.logAnalyticsEvent(Utils.ANALYTICS_EVENTS.CONFIG_BACKUPCLOUD_RESTORE)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                DatabaseHelper.getInstance().replaceLiveUserDataFromFile(cloudRestoreFile)
                Utils.toast(Res.string.dbrestore_success)
            } catch (e: IllegalStateException) {
                Utils.toast(Res.string.dbrestore_failure_fileformat)
                Utils.logAnalyticsError(
                    "BACKUP_RESTORE",
                    getString(Res.string.dbrestore_failure_fileformat),
                    e
                )
            } catch (e: Exception) {
                Utils.toast(Res.string.dbrestore_failure_import)
                Utils.logAnalyticsError(
                    "BACKUP_RESTORE",
                    getString(Res.string.dbrestore_failure_import),
                    e
                )
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
