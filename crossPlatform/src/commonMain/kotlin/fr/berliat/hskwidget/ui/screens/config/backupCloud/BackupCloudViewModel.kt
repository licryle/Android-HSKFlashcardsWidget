package fr.berliat.hskwidget.ui.screens.config.backupCloud

import fr.berliat.googledrivebackup.GoogleDriveBackup
import fr.berliat.hskwidget.core.HSKAppServices
import fr.berliat.hskwidget.data.store.AppPreferencesStore

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.datetime.Instant

expect class BackupCloudViewModel(
    appConfig: AppPreferencesStore = HSKAppServices.appPreferences,
    gDriveBackup: GoogleDriveBackup
) {
    val cloudLastBackup: StateFlow<Instant>  // adjust type if needed
    val isBusy: StateFlow<Boolean>
    val transferState: StateFlow<BackupCloudTransferEvent?>
    val restoreFileFrom: MutableStateFlow<Instant?>

    fun backup()
    fun restore()
    fun confirmRestoration()
    fun cancel()
}