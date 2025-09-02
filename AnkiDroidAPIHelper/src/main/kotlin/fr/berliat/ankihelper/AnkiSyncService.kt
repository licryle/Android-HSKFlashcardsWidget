package fr.berliat.ankidroidhelper

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.graphics.Bitmap
import android.os.Binder
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Service for handling long operations with progress tracking and cancellation support.
 * 
 * This service provides:
 * - Progress tracking via StateFlow
 * - Cancellation support
 * - Notification with progress updates
 * - Background execution
 */
abstract class AnkiSyncService : LifecycleService() {
    companion object {
        private const val TAG = "AnkiSyncService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "long_operation_channel"
        
        // Intent actions
        const val ACTION_START_OPERATION = "fr.berliat.hskwidget.START_OPERATION"
        const val ACTION_CANCEL_OPERATION = "fr.berliat.hskwidget.CANCEL_OPERATION"
        const val ACTION_STOP_SERVICE = "fr.berliat.hskwidget.STOP_SERVICE"

        // Intent extras
        const val EXTRA_OPERATION_TYPE = "operation_type"
        const val EXTRA_OPERATION_DATA = "operation_data"
        
        // Operation types
        const val OPERATION_SYNC_TO_ANKI = "sync_to_anki"
    }
    
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var currentJob: Job? = null
    
    private val _operationState = MutableStateFlow<OperationState>(OperationState.Idle)
    val operationState: StateFlow<OperationState> = _operationState.asStateFlow()
    
    private val binder = LongOperationBinder()

    private var notificationManager: NotificationManager? = null
    
    inner class LongOperationBinder : Binder() {
        fun getService(): AnkiSyncService = this@AnkiSyncService
    }
    
    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
    }

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        when (intent?.action) {
            ACTION_START_OPERATION -> {
                val operationType = intent.getStringExtra(EXTRA_OPERATION_TYPE)
                val operationData = intent.getStringExtra(EXTRA_OPERATION_DATA)
                startOperation(operationType, operationData)
            }
            ACTION_CANCEL_OPERATION -> {
                cancelCurrentOperation()
            }
            ACTION_STOP_SERVICE -> {
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }
    
    override fun onDestroy() {
        super.onDestroy()
        cancelCurrentOperation()
        serviceScope.cancel()
    }
    
    private fun startOperation(operationType: String?, operationData: String?) {
        if (currentJob?.isActive == true) {
            Log.w(TAG, "Operation already in progress, ignoring start request")
            return
        }

        when (operationType) {
            OPERATION_SYNC_TO_ANKI -> {
                startSyncToAnkiOperation(operationData)
            }
            else -> {
                Log.e(TAG, "Unknown operation type: $operationType")
                _operationState.value =
                    OperationState.Error("Unknown operation type: $operationType")
            }
        }
    }

    protected abstract suspend fun syncToAnki()
    protected abstract fun getSyncStartMessage() : String
    abstract fun getActivityClass(): Class<out Any>
    abstract fun getNotificationTitle(): String
    abstract fun getNotificationLargeIcon(): Bitmap?
    abstract fun getNotificationSmallIcon(): Int
    abstract fun getNotificationCancelIcon(): Int
    abstract fun getNotificationCancelText(): String
    abstract fun getNotificationChannelTitle(): String
    abstract fun getNotificationChannelDescription(): String


    private fun startSyncToAnkiOperation(operationData: String?) {
        currentJob = serviceScope.launch(Dispatchers.IO) {
            try {
                _operationState.value = OperationState.Running(
                    operationType = OPERATION_SYNC_TO_ANKI,
                    progress = 0,
                    total = 0,
                    errors = 0,
                    message = getSyncStartMessage()
                )
                
                // Start foreground service with notification
                startForeground(NOTIFICATION_ID,
                    createNotification(0, 0, 0, getSyncStartMessage()))

                syncToAnki()

                _operationState.value = OperationState.Completed
            } catch (_: CancellationException) {
                Log.i(TAG, "Sync operation cancelled")
                _operationState.value = OperationState.Cancelled
            } catch (e: Exception) {
                Log.e(TAG, "Sync operation failed", e)
                _operationState.value = OperationState.Error(e.message ?: "Unknown error")
            } finally {
                clearNotification()
                stopForeground(true)
                _operationState.value = OperationState.Idle // Awaiting a new activation or destruction
            }
        }
    }
    
    private fun cancelCurrentOperation() {
        currentJob?.cancel()
        currentJob = null
        stopForeground(true)
        _operationState.value = OperationState.Cancelled
        _operationState.value = OperationState.Idle
        clearNotification()
    }
    
    protected suspend fun updateProgress(current: Int, errors: Int, total: Int, message: String) {
        _operationState.value = OperationState.Running(
            operationType = (_operationState.value as? OperationState.Running)?.operationType
                ?: "",
            progress = current,
            errors = errors,
            total = total,
            message = message
        )

        // Update notification
        notificationManager?.notify(NOTIFICATION_ID, createNotification(current, errors, total, message))
    }
    
    private fun createNotification(progress: Int, errors: Int, total: Int, message: String): Notification {
        val intent = Intent(this, getActivityClass()).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val cancelIntent = Intent(this, this::class.java).apply {
            action = ACTION_CANCEL_OPERATION
        }
        
        val cancelPendingIntent = PendingIntent.getService(
            this, 1, cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getNotificationTitle())
            .setContentText(message)
            .setLargeIcon(getNotificationLargeIcon())
            .setSmallIcon(getNotificationSmallIcon())
            .setContentIntent(pendingIntent)
            .addAction(getNotificationCancelIcon(), getNotificationCancelText(), cancelPendingIntent)
            .setProgress(if (total > 0) total else 0, progress, total == 0)
            .build()
    }

    private fun clearNotification() {
        notificationManager?.cancel(NOTIFICATION_ID)
    }
    
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getNotificationChannelTitle(),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getNotificationChannelDescription()
            setShowBadge(false)
        }

        notificationManager?.createNotificationChannel(channel)
    }
    
    sealed class OperationState {
        object Idle : OperationState()
        data class Running(
            val operationType: String,
            val progress: Int,
            val errors: Int,
            val total: Int,
            val message: String
        ) : OperationState()
        object Completed : OperationState()
        object Cancelled : OperationState()
        data class Error(val message: String) : OperationState()
    }
} 