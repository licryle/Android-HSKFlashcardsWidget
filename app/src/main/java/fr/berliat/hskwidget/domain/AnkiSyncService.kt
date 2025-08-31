package fr.berliat.hskwidget.domain

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.database.sqlite.SQLiteException
import android.graphics.BitmapFactory
import android.os.Binder
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import fr.berliat.hskwidget.MainActivity
import fr.berliat.hskwidget.R
import fr.berliat.hskwidget.data.model.AnnotatedChineseWord
import fr.berliat.hskwidget.data.model.WordList
import fr.berliat.hskwidget.data.repo.WordListRepository
import fr.berliat.hskwidget.data.repo.WordListRepository.SharedEventBus.UiEvent
import fr.berliat.hskwidget.data.store.AnkiStore
import fr.berliat.hskwidget.data.store.DatabaseHelper
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
import kotlinx.coroutines.yield
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

/**
 * Service for handling long operations with progress tracking and cancellation support.
 * 
 * This service provides:
 * - Progress tracking via StateFlow
 * - Cancellation support
 * - Notification with progress updates
 * - Background execution
 */
class AnkiSyncService : LifecycleService() {
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

    private suspend fun syncToAnki() {
        val wordListDAO = DatabaseHelper.getInstance(this).wordListDAO()
        val annotatedChineseWordDAO = DatabaseHelper.getInstance(this).annotatedChineseWordDAO()
        val ankiStore = AnkiStore(this)

        val lists = wordListDAO.getAllLists()
        val entries = wordListDAO.getAllListEntries()

        val annotatedChineseWords = mutableListOf<AnnotatedChineseWord>()
        val wordList = entries.map { it.simplified }

        /**
         * Some devices like Huawei P20 or Samsung Galaxy A11 have a low limit on variables per
         * SQLite queries. Most devices don't, so to be safe, we will iterate at max 12 times with
         * lowering the number of words per query, hence lowering the number of variables each time.
         */
        var chunkSize = 2048
        var formerException : SQLiteException? = SQLiteException()
        while (formerException != null && chunkSize >= 1) {
            annotatedChineseWords.clear()

            try {
                // Safeguard against too many SQL vars error for a big migration
                val chunks = wordList.chunked(chunkSize)
                for (chunk in chunks) {
                    val partialResult = annotatedChineseWordDAO.getFromSimplified(chunk)
                    annotatedChineseWords.addAll(partialResult)
                }
                formerException = null

            } catch (e: SQLiteException) {
                formerException = e
                chunkSize /= 2
            }
        }

        if (formerException != null) {
            throw formerException
        }

        val words = annotatedChineseWords.associateBy { it.simplified }

        if (lists.isEmpty() || entries.isEmpty() || words.isEmpty()) return
        val nbToImport = entries.size

        // Update notification and Emit progress event
        updateProgress(0, 0, nbToImport, getString(R.string.anki_import_started))

        var nbErrors = 0

        var nbDeckCreationErrors = 0
        Log.i(WordListRepository.TAG, "syncListsToAnki.Anki: Creating decks if needed")
        val decks = mutableMapOf<Long, AnkiDeck>()
        for (list in lists) {
            decks[list.id] = AnkiDeck.getOrCreate(this, ankiStore, list.wordList)

            if (decks[list.id]!!.ankiId == WordList.ANKI_ID_EMPTY) {
                nbDeckCreationErrors += 1
            }
        }
        Log.i(WordListRepository.TAG, "syncListsToAnki.Anki: Created decks if possible: $nbDeckCreationErrors errors")

        Log.i(WordListRepository.TAG, "syncListsToAnki.Anki: Starting iterating through lists")
        for ((index, entry) in entries.withIndex()) {
            val deck = decks[entry.listId]
            if (deck == null) {
                Log.e(WordListRepository.TAG, "syncListsToAnki.Anki: found a WordListEntry not linked to any list. Skipping entry")
                nbErrors += 1
                continue
            }

            if (words[entry.simplified] == null) {
                Log.e(WordListRepository.TAG, "syncListsToAnki.Anki: found a WordListEntry not linked to an actual word. Skipping entry")
                nbErrors += 1
                continue
            }

            if (deck.ankiId == WordList.ANKI_ID_EMPTY) {
                Log.e(WordListRepository.TAG, "syncListsToAnki.Anki: deck with no ID in Anki. Skipping entry")
                nbErrors += 1
                continue
            }

            val id = ankiStore.importOrUpdateCard(deck, entry, words[entry.simplified]!!)

            if (id == null) {
                nbErrors += 1
            }

            // Report progress
            val progress = index + 1
            var message = resources.getString(R.string.anki_import_progress)
            message = message.format(entry.simplified, progress, nbToImport)

            Log.d(WordListRepository.TAG, message)

            currentCoroutineContext().ensureActive() // let the loop stop IF asked to cancel
            // Update notification and Emit progress event
            updateProgress(progress, nbErrors, nbToImport, message)
            yield() // Ensure other small operations can happen. Shouldn't be needed though.
        }

        Log.i(WordListRepository.TAG, "importOrUpdateAllCards: imported for {$nbToImport-$nbErrors} ouf of $nbToImport")

        if (nbErrors > 0) {
            Result.failure(Exception("$nbErrors ouf of $nbToImport imported to Anki."))
        } else {
            Result.success(Unit)
        }
    }
    
    private fun startSyncToAnkiOperation(operationData: String?) {
        currentJob = serviceScope.launch(Dispatchers.IO) {
            try {
                _operationState.value = OperationState.Running(
                    operationType = OPERATION_SYNC_TO_ANKI,
                    progress = 0,
                    total = 0,
                    errors = 0,
                    message = getString(R.string.anki_import_started)
                )
                
                // Start foreground service with notification
                startForeground(NOTIFICATION_ID,
                    createNotification(0, 0, 0, getString(R.string.anki_import_started)))

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
    
    private suspend fun updateProgress(current: Int, errors: Int, total: Int, message: String) {
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

        WordListRepository.SharedEventBus.emit(UiEvent.ProgressUpdate(_operationState.value as OperationState.Running))
    }
    
    private fun createNotification(progress: Int, errors: Int, total: Int, message: String): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val cancelIntent = Intent(this, AnkiSyncService::class.java).apply {
            action = ACTION_CANCEL_OPERATION
        }
        
        val cancelPendingIntent = PendingIntent.getService(
            this, 1, cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(message)
            .setLargeIcon(BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher))
            .setSmallIcon(R.mipmap.ic_launcher_monochrome_mini)
            .setContentIntent(pendingIntent)
            .addAction(R.drawable.close_24px, getString(R.string.cancel), cancelPendingIntent)
            .setProgress(if (total > 0) total else 0, progress, total == 0)
            .build()
    }

    private fun clearNotification() {
        notificationManager?.cancel(NOTIFICATION_ID)
    }
    
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.anki_sync_notification_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.anki_sync_notification_description)
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