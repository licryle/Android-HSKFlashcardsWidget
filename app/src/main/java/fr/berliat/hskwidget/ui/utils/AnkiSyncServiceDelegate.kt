package fr.berliat.hskwidget.ui.utils

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import fr.berliat.hskwidget.domain.AnkiSyncService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Delegate for handling long operations with progress tracking and cancellation support.
 * 
 * This delegate provides a clean interface to:
 * - Start long operations via service
 * - Monitor progress via StateFlow
 * - Cancel operations
 * - Handle service lifecycle
 */
class AnkiSyncServiceDelegate(
    private val context: Context
) {
    companion object {
        private const val TAG = "AnkiSyncServiceDelegate"
    }
    
    private var service: AnkiSyncService? = null
    private var isBound = false
    private var pendingStart: Boolean = false
    
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val longOperationBinder = binder as AnkiSyncService.LongOperationBinder
            service = longOperationBinder.getService()
            isBound = true
            Log.d(TAG, "Service connected")

            if (pendingStart) {
                pendingStart = false
                startSyncToAnkiOperation() // retry now that we're bound
            }
        }
        
        override fun onServiceDisconnected(name: ComponentName?) {
            service = null
            isBound = false
            Log.d(TAG, "Service disconnected")
        }
    }
    
    init {
        bindService()
        observeOperationState()
    }
    
    /**
     * Start a sync to Anki operation
     */
    fun startSyncToAnkiOperation() {
        if (!isBound) {
            Log.w(TAG, "Service not bound, queuing start ")
            pendingStart = true
            return
        }
        
        val intent = Intent(context, AnkiSyncService::class.java).apply {
            action = AnkiSyncService.ACTION_START_OPERATION
            putExtra(AnkiSyncService.EXTRA_OPERATION_TYPE, AnkiSyncService.OPERATION_SYNC_TO_ANKI)
        }
        
        context.startService(intent)
        Log.d(TAG, "Started sync to Anki operation")
    }
    
    /**
     * Cancel the current operation
     */
    fun cancelCurrentOperation() {
        if (!isBound) {
            Log.w(TAG, "Service not bound, cannot cancel operation")
            return
        }
        
        val intent = Intent(context, AnkiSyncService::class.java).apply {
            action = AnkiSyncService.ACTION_CANCEL_OPERATION
        }
        
        context.startService(intent)
        Log.d(TAG, "Cancelled current operation")
    }
    
    /**
     * Get the current operation state
     */
    fun getOperationState(): StateFlow<AnkiSyncService.OperationState>? {
        return service?.operationState
    }

    /**
     * Return the end state only when done to propagate to AnkiDelegate (in most scenarios).
     */
    suspend fun awaitOperationCompletion(): Result<Unit> {
        while (service == null) {
            delay(50)
        }

        val state =
            service?.operationState
                ?.filterNotNull()
                ?.first { it is AnkiSyncService.OperationState.Completed
                    || it is AnkiSyncService.OperationState.Cancelled
                    || it is AnkiSyncService.OperationState.Error }

        return when (state) {
            is AnkiSyncService.OperationState.Completed -> Result.success(Unit)
            is AnkiSyncService.OperationState.Cancelled -> Result.failure(CancellationException())
            is AnkiSyncService.OperationState.Error -> Result.failure(Exception(state.message))
            else -> Result.failure(Exception("Unexpected state"))
        }
    }
    
    private fun bindService() {
        val intent = Intent(context, AnkiSyncService::class.java)
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }
    
    private fun unbindService() {
        if (isBound) {
            context.unbindService(serviceConnection)
            isBound = false
        }
    }

    private fun observeOperationState() {
        val lifecycleOwner = ProcessLifecycleOwner.get()

        lifecycleOwner.lifecycleScope.launch {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                service?.operationState?.collect { state ->
                    when (state) {
                        is AnkiSyncService.OperationState.Idle -> {
                            Log.d(TAG, "Operation state: Idle")
                        }
                        is AnkiSyncService.OperationState.Running -> {
                            Log.d(TAG, "Operation state: Running - ${state.progress}/${state.total} - ${state.message}")
                        }
                        is AnkiSyncService.OperationState.Completed -> {
                            Log.d(TAG, "Operation state: Completed")
                        }
                        is AnkiSyncService.OperationState.Cancelled -> {
                            Log.d(TAG, "Operation state: Cancelled")
                        }
                        is AnkiSyncService.OperationState.Error -> {
                            Log.e(TAG, "Operation state: Error - ${state.message}")
                        }
                    }
                }
            }
        }
    }
    
    fun cleanup() {
        unbindService()
    }
} 