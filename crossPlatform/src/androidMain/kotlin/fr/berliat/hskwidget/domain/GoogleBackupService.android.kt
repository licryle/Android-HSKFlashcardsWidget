package fr.berliat.hskwidget.domain

import android.content.Intent
import co.touchlab.kermit.Logger
import fr.berliat.hskwidget.core.ExpectedUtils

actual object GoogleBackupService {
    actual fun startBackup() {
        val context = ExpectedUtils.context
        val intent = Intent(context, GoogleBackupUploadService::class.java).apply {
            action = GoogleBackupUploadService.ACTION_START_UPLOAD
        }
        try {
            context.startForegroundService(intent)
        } catch (e: Exception) {
            Logger.e(tag = TAG, messageString = "Failed to start GoogleBackupUploadService", throwable = e)
        }
    }

    actual fun startRestore() {
        val context = ExpectedUtils.context
        val intent = Intent(context, GoogleBackupDownloadService::class.java).apply {
            action = GoogleBackupDownloadService.ACTION_START_DOWNLOAD
        }
        try {
            context.startForegroundService(intent)
        } catch (e: Exception) {
            Logger.e(tag = TAG, messageString = "Failed to start GoogleBackupDownloadService", throwable = e)
        }
    }

    private const val TAG = "GoogleBackupService"
}
