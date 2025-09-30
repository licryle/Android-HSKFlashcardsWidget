package fr.berliat.hskwidget.ui.screens.config.backupCloud

import fr.berliat.hskwidget.core.HSKAppServices
import fr.berliat.hskwidget.data.store.AppPreferencesStore
import fr.berliat.hskwidget.data.store.GoogleDriveBackup

import kotlinx.coroutines.flow.StateFlow
import kotlinx.datetime.Instant

expect class BackupCloudViewModel(
    appConfig: AppPreferencesStore = HSKAppServices.appPreferences,
    gDriveBackup: GoogleDriveBackup
) {
    val cloudLastBackup: StateFlow<Instant>  // adjust type if needed
    val isBusy: StateFlow<Boolean>
    val transferState: StateFlow<BackupCloudTransferEvent?>

    fun backup()
    fun restore()
    fun cancel()
}