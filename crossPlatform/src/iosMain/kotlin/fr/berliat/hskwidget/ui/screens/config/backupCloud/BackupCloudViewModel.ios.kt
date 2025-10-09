package fr.berliat.hskwidget.ui.screens.config.backupCloud

import fr.berliat.hskwidget.data.store.AppPreferencesStore
import fr.berliat.hskwidget.data.store.GoogleDriveBackup
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.datetime.Instant

actual class BackupCloudViewModel actual constructor(
    appConfig: AppPreferencesStore,
    gDriveBackup: GoogleDriveBackup
) {
    actual val cloudLastBackup: StateFlow<Instant>
        get() = TODO("Not yet implemented")
    actual val isBusy: StateFlow<Boolean>
        get() = TODO("Not yet implemented")
    actual val transferState: StateFlow<BackupCloudTransferEvent?>
        get() = TODO("Not yet implemented")
    actual val restoreFileFrom: MutableStateFlow<Instant?>
        get() = TODO("Not yet implemented")

    actual fun backup() {
    }

    actual fun restore() {
    }

    actual fun confirmRestoration() {
    }

    actual fun cancel() {
    }
}