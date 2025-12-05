package fr.berliat.hskwidget.ui.screens.config.backupCloud

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import fr.berliat.googledrivebackup.BackupEvent
import fr.berliat.googledrivebackup.GoogleDriveBackupFile
import fr.berliat.googledrivebackup.GoogleDriveState
import fr.berliat.googledrivebackup.RestoreEvent
import fr.berliat.hskwidget.core.Utils
import fr.berliat.hskwidget.data.store.AppPreferencesStore
import fr.berliat.hskwidget.domain.DatabaseHelper
import fr.berliat.hskwidget.data.store.GoogleDriveBackup
import fr.berliat.hskwidget.ui.screens.config.backupCloud.BackupCloudTransferEvent.*

import fr.berliat.hskwidget.Res
import fr.berliat.hskwidget.core.HSKAppServices
import fr.berliat.hskwidget.core.Logging
import fr.berliat.hskwidget.data.store.snapshotToFile
import fr.berliat.hskwidget.dbrestore_failure_fileformat
import fr.berliat.hskwidget.dbrestore_failure_import
import fr.berliat.hskwidget.dbrestore_start
import fr.berliat.hskwidget.dbrestore_success

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
            viewModelScope.launch(Dispatchers.IO) {
                val gDriveBackupSnapshot = DatabaseHelper.getInstance().liveDatabase.snapshotToFile()

                if (gDriveBackupSnapshot == null) {
                    _transferState.emit(BackupFailed(Exception("Database snapshot failed")))
                    return@launch
                }
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
                        is BackupEvent.Cancelled -> _transferState.emit(BackupCancelled)
                        is BackupEvent.Failed -> _transferState.emit(BackupFailed(event.exception))
                        is BackupEvent.Progress -> _transferState.emit(
                            BackupProgress(
                                fileIndex = event.fileIndex,
                                fileCount = event.fileCount,
                                bytesReceived = event.bytesSent,
                                bytesTotal = event.bytesTotal
                            )
                        )

                        is BackupEvent.Started -> _transferState.emit(BackupStarted)
                        is BackupEvent.Success -> {
                            _transferState.emit(BackupSuccess)
                            appConfig.dbBackupCloudLastSuccess.value = Clock.System.now()
                            gDriveBackup.deletePreviousBackups()
                        }
                    }
                }
            }
        }

        Logging.logAnalyticsEvent(Logging.ANALYTICS_EVENTS.CONFIG_BACKUPCLOUD_BACKUP)
    }

    actual fun restore() {
        gDriveBackup.login {
            viewModelScope.launch(Dispatchers.IO) {
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
                        is RestoreEvent.Cancelled -> _transferState.emit(RestorationCancelled)
                        is RestoreEvent.Failed -> _transferState.emit(RestorationFailed(event.exception))
                        is RestoreEvent.Progress -> _transferState.emit(
                            RestorationProgress(
                                fileIndex = event.fileIndex,
                                fileCount = event.fileCount,
                                bytesReceived = event.bytesReceived,
                                bytesTotal = event.bytesTotal
                            )
                        )

                        is RestoreEvent.Started -> _transferState.emit(RestorationStarted)
                        is RestoreEvent.Success -> {
                            _transferState.emit(RestorationSuccess)

                            if (event.files.isEmpty() || event.files[0].name != "database.sqlite") {
                                throw Exception("ConfigFragement.onRestoreSuccess: Something went really wrong in GoogleDriveBackUp lib, wrong backup file")
                            }

                            restoreFileFrom.value = Instant.fromEpochSeconds(event.files[0].modifiedTime?.epochSecond ?: 0)
                        }
                        is RestoreEvent.Empty -> _transferState.emit(RestorationFailed(Exception("No Backup File")))
                    }
                }
            }
        }
    }

    actual fun confirmRestoration() {
        HSKAppServices.snackbar.show(Res.string.dbrestore_start)

        Logging.logAnalyticsEvent(Logging.ANALYTICS_EVENTS.CONFIG_BACKUPCLOUD_RESTORE)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                DatabaseHelper.getInstance().replaceLiveUserDataFromFile(cloudRestoreFile)
                HSKAppServices.snackbar.show(Res.string.dbrestore_success)
            } catch (e: IllegalStateException) {
                HSKAppServices.snackbar.show(Res.string.dbrestore_failure_fileformat)
                Logging.logAnalyticsError(
                    "BACKUP_RESTORE",
                    getString(Res.string.dbrestore_failure_fileformat),
                    e.toString()
                )
            } catch (e: Exception) {
                HSKAppServices.snackbar.show(Res.string.dbrestore_failure_import)
                Logging.logAnalyticsError(
                    "BACKUP_RESTORE",
                    getString(Res.string.dbrestore_failure_import),
                    e.toString()
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
