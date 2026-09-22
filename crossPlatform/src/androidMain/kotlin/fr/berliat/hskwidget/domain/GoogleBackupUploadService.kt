package fr.berliat.hskwidget.domain

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import fr.berliat.hskwidget.Res
import fr.berliat.hskwidget.core.AppDispatchers
import fr.berliat.hskwidget.core.ExpectedUtils
import fr.berliat.hskwidget.googledrive_backup_start_title
import fr.berliat.hskwidget.googledrive_backup_start_message
import fr.berliat.hskwidget.googledrive_backup_cancel_message
import fr.berliat.hskwidget.googledrive_backup_cancel_title
import fr.berliat.hskwidget.googledrive_backup_failed_message
import fr.berliat.hskwidget.googledrive_backup_failed_title
import fr.berliat.hskwidget.googledrive_backup_success_message
import fr.berliat.hskwidget.googledrive_backup_success_title
import fr.berliat.hskwidget.ui.screens.config.backupCloud.BackupCloudTransferEvent
import fr.berliat.hskwidget.core.YYMMDDHHMMSS
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import org.jetbrains.compose.resources.getString

class GoogleBackupUploadService : LifecycleService() {
    companion object {
        private const val TAG = "GoogleBackupUploadServ"
        private const val NOTIFICATION_ID = 1003
        private const val CHANNEL_ID = "google_backup_upload_channel"

        const val ACTION_START_UPLOAD = "fr.berliat.hskwidget.START_GOOGLE_BACKUP_UPLOAD"
    }

    private var notificationManager: NotificationManager? = null
    private var notificationTitle: String? = null
    private var isUploading = false

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationTitle = runBlocking { getString(Res.string.googledrive_backup_start_title) }
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if (intent?.action == ACTION_START_UPLOAD) {
            if (!isUploading) {
                val title = notificationTitle ?: runBlocking { getString(Res.string.googledrive_backup_start_title) }
                val message = runBlocking { getString(Res.string.googledrive_backup_start_message) }
                val notification = createNotification(null, message, title)
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    startForeground(NOTIFICATION_ID, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
                } else {
                    startForeground(NOTIFICATION_ID, notification)
                }
                startUpload()
            }
        }
        return START_NOT_STICKY
    }

    private fun startUpload() {
        isUploading = true
        lifecycleScope.launch(AppDispatchers.IO) {
            GoogleBackupSharedLogic.runBackupInternal { progressPercent, message ->
                updateNotification(progressPercent, message, notificationTitle ?: "")
            }

            // Finalize notification based on final state
            val finalState = GoogleBackupFlowState.globalTransferState.value
            when (finalState) {
                is BackupCloudTransferEvent.BackupSuccess -> {
                    val time = Clock.System.now().YYMMDDHHMMSS()
                    showFinalNotification(
                        getString(Res.string.googledrive_backup_success_title),
                        getString(Res.string.googledrive_backup_success_message, time)
                    )
                }
                is BackupCloudTransferEvent.BackupCancelled -> {
                    showFinalNotification(
                        getString(Res.string.googledrive_backup_cancel_title),
                        getString(Res.string.googledrive_backup_cancel_message)
                    )
                }
                is BackupCloudTransferEvent.BackupFailed -> {
                    showFinalNotification(
                        getString(Res.string.googledrive_backup_failed_title),
                        getString(Res.string.googledrive_backup_failed_message, finalState.exception.message ?: "Unknown error")
                    )
                }
                else -> {}
            }

            isUploading = false
            stopForeground(STOP_FOREGROUND_DETACH)
            stopSelf()
        }
    }

    private fun showFinalNotification(title: String, message: String) {
        val notification = createNotification(null, message, title, ongoing = false)
        notificationManager?.notify(NOTIFICATION_ID, notification)
    }

    private fun updateNotification(progress: Float?, message: String, title: String) {
        notificationManager?.notify(NOTIFICATION_ID, createNotification(progress, message, title))
    }

    private fun createNotification(progress: Float?, message: String, title: String, ongoing: Boolean = true): Notification {
        val intent = ExpectedUtils.context.packageManager.getLaunchIntentForPackage(packageName)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(this.resources.getIdentifier("ic_launcher", "mipmap", this.packageName))
            .setContentIntent(pendingIntent)
            .setOngoing(ongoing)
            .setOnlyAlertOnce(true)

        if (ongoing) {
            if (progress != null) {
                builder.setProgress(100, progress.toInt(), false)
            } else {
                builder.setProgress(100, 0, true)
            }
        } else {
            builder.setProgress(0, 0, false)
        }

        return builder.build()
    }

    private fun createNotificationChannel() {
        val name = notificationTitle ?: runBlocking { getString(Res.string.googledrive_backup_start_title) }
        val importance = NotificationManager.IMPORTANCE_LOW
        val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
            setShowBadge(false)
        }
        notificationManager?.createNotificationChannel(channel)
    }
}
