package fr.berliat.hskwidget.domain

import android.content.Intent
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
            android.util.Log.e("GoogleBackupService", "Failed to start GoogleBackupUploadService", e)
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
            android.util.Log.e("GoogleBackupService", "Failed to start GoogleBackupDownloadService", e)
        }
    }
}
