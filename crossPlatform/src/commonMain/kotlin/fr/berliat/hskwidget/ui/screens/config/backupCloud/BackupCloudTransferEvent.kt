package fr.berliat.hskwidget.ui.screens.config.backupCloud

sealed class BackupCloudTransferEvent {
    object Started : BackupCloudTransferEvent()
    data class Progress(
        val fileIndex: Int,
        val fileCount: Int,
        val bytesReceived: Long,
        val bytesTotal: Long
    ) : BackupCloudTransferEvent()
    object Success : BackupCloudTransferEvent()
    object Cancelled : BackupCloudTransferEvent()
    data class Failed(val exception: Exception) : BackupCloudTransferEvent()
}