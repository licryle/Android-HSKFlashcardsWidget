package fr.berliat.hskwidget.domain

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import fr.berliat.hskwidget.Res
import fr.berliat.hskwidget.core.AppDispatchers
import fr.berliat.hskwidget.core.ExpectedUtils
import fr.berliat.hskwidget.database_update_notification_description
import fr.berliat.hskwidget.database_update_notification_name
import fr.berliat.hskwidget.database_update_starting
import fr.berliat.hskwidget.database_updating_progress
import fr.berliat.hskwidget.loading
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString
import kotlin.time.Duration.Companion.milliseconds

class DatabaseUpdateService : LifecycleService() {
    companion object {
        private const val TAG = "DatabaseUpdateService"
        private const val NOTIFICATION_ID = 1002
        private const val CHANNEL_ID = "database_update_channel"

        const val ACTION_START_UPDATE = "fr.berliat.hskwidget.START_DATABASE_UPDATE"
        const val EXTRA_FORCE_REPAIR = "force_repair"
    }

    private var notificationManager: NotificationManager? = null
    private var notificationTitle: String? = null
    private var isUpdating = false

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationTitle = runBlocking { getString(Res.string.database_update_notification_name) }
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if (intent?.action == ACTION_START_UPDATE) {
            val force = intent.getBooleanExtra(EXTRA_FORCE_REPAIR, false)
            if (!isUpdating) {
                val title = notificationTitle ?: runBlocking { getString(Res.string.database_update_notification_name) }
                val message = runBlocking { getString(Res.string.database_update_starting) }
                val notification = createNotification(null, message, title)
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    startForeground(NOTIFICATION_ID, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
                } else {
                    startForeground(NOTIFICATION_ID, notification)
                }
                startUpdate(force)
            }
        }
        return START_NOT_STICKY
    }

    private fun startUpdate(force: Boolean) {
        isUpdating = true
        lifecycleScope.launch(AppDispatchers.IO) {
            try {
                var retryCount = 0
                while (notificationTitle == null && retryCount < 20) {
                    kotlinx.coroutines.delay(100.milliseconds)
                    retryCount++
                }

                val title = notificationTitle ?: getString(Res.string.database_update_notification_name)
                val startMessage = getString(Res.string.loading)
                updateNotification(null, startMessage, title)

                // Observe progress to update notification
                val progressJob = launch {
                    DatabaseHelper.updateProgress.collectLatest { progress ->
                        val message = if (progress != null) {
                            getString(Res.string.database_updating_progress, progress.toInt())
                        } else {
                            getString(Res.string.loading)
                        }
                        updateNotification(progress, message, title)
                    }
                }

                DatabaseHelper.getInstance().runDatabaseUpdateNow(force = force)

                progressJob.cancel()
                isUpdating = false
                stopSelf()
            } catch (e: Exception) {
                Log.e(TAG, "Error in startUpdate", e)
                isUpdating = false
                stopSelf()
            }
        }
    }

    private fun updateNotification(progress: Float?, message: String, title: String) {
        notificationManager?.notify(NOTIFICATION_ID, createNotification(progress, message, title))
    }

    private fun createNotification(progress: Float?, message: String, title: String): Notification {
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
            .setOngoing(true)
            .setOnlyAlertOnce(true)

        if (progress != null) {
            builder.setProgress(100, progress.toInt(), false)
        } else {
            builder.setProgress(100, 0, true)
        }

        return builder.build()
    }

    private fun createNotificationChannel() {
        val name = notificationTitle ?: runBlocking { getString(Res.string.database_update_notification_name) }
        val descriptionText = runBlocking { getString(Res.string.database_update_notification_description) }
        val importance = NotificationManager.IMPORTANCE_LOW
        val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
            description = descriptionText
            setShowBadge(false)
        }
        notificationManager?.createNotificationChannel(channel)
    }
}
