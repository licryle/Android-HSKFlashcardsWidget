package fr.berliat.hskwidget.domain

import fr.berliat.hskwidget.ui.screens.config.backupCloud.BackupCloudTransferEvent
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.datetime.Instant

object GoogleBackupFlowState {
    val globalTransferState = MutableStateFlow<BackupCloudTransferEvent?>(null)
    val globalRestoreFileFrom = MutableStateFlow<Instant?>(null)
    var globalCloudRestoreFile: PlatformFile? = null
}
