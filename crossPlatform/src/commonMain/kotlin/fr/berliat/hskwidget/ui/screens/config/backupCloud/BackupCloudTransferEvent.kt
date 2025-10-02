package fr.berliat.hskwidget.ui.screens.config.backupCloud

sealed class BackupCloudTransferEvent {
    object RestorationStarted : BackupCloudTransferEvent()
    data class RestorationProgress(
        val fileIndex: Int,
        val fileCount: Int,
        val bytesReceived: Long,
        val bytesTotal: Long
    ) : BackupCloudTransferEvent()
    object RestorationSuccess : BackupCloudTransferEvent()
    object RestorationCancelled : BackupCloudTransferEvent()
    data class RestorationFailed(val exception: Exception) : BackupCloudTransferEvent()
    object BackupStarted : BackupCloudTransferEvent()
    data class BackupProgress(
        val fileIndex: Int,
        val fileCount: Int,
        val bytesReceived: Long,
        val bytesTotal: Long
    ) : BackupCloudTransferEvent()
    object BackupSuccess : BackupCloudTransferEvent()
    object BackupCancelled : BackupCloudTransferEvent()
    data class BackupFailed(val exception: Exception) : BackupCloudTransferEvent()
}