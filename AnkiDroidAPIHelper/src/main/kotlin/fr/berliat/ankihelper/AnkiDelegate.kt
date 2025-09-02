package fr.berliat.ankidroidhelper

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.ichi2.anki.api.AddContentApi
import com.ichi2.anki.api.AddContentApi.READ_WRITE_PERMISSION
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * AnkiDelegate is your easiest way to store things into Anki. This implementation is
 * coupled to the WordListManager.
 *
 * Due to the fact that Anki API can only be called after having the right permissions, and app
 * is running. There are lots of checks to be done, and the permissions one must happen in the
 * fragment thread. This Helper handles it all. All you focus on is returning suspend methods that
 * do the actual Anki API calls.
 *
 * Here's a simplistic example:
 *     override fun onCreate(savedInstanceState: Bundle?) {
 *         super.onCreate(savedInstanceState)
 *
 *         ankiDelegate = AnkiDelegate(this)
 *         ankiDelegate.delegateToAnki(WordListRepo.insertWordToList(list, word))
 *     }
 *
 * If you use a viewModel, make sure to only pass the ankiDelegate::delegateToAnki method to not
 * create memory leaks. AnkiDelegate does reference a fragment after all. That function has a helper
 * signature typealias called "AnkiDelegator".
 *
 * Beware of execution patterns, as the callbacks can mean Anki calls executing after whatever
 * element you change/delete.
 */
typealias AnkiDelegator = suspend ((suspend () -> Result<Unit>)?) -> Unit

open class AnkiDelegate(
    private val fragment: Fragment
) {

    interface HandlerInterface {
        fun onAnkiOperationSuccess()
        fun onAnkiOperationCancelled()
        fun onAnkiOperationFailed(e: Throwable)
        fun onAnkiSyncProgress(current: Int, total: Int, message: String)
        fun onAnkiRequestPermissionGranted()
        fun onAnkiRequestPermissionDenied()
        fun onAnkiServiceStarting(serviceDelegate: AnkiSyncServiceDelegate)
    }

    private val lifecycleOwner : LifecycleOwner = fragment
    private val context = fragment.requireContext()
    private val callbackHandler = fragment as? HandlerInterface
    private val callQueue: ArrayDeque<suspend () -> Result<Unit>> = ArrayDeque()
    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>

    init {
        initPermissionHandling { isGranted -> onAnkiRequestPermissionsResult(isGranted) }
        observeUiEvents()
    }

    suspend fun delegateToAnki(ankiAction: (suspend () -> Result<Unit>)?) {
        ankiAction?.let { AnkiSharedEventBus.emit(AnkiSharedEventBus.UiEvent.AnkiAction(it)) }
    }

    /********** Anki Permissions ************/
    protected fun initPermissionHandling(callback: (Boolean) -> Unit) {
        permissionLauncher = fragment.registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()) { result ->
            callback(result[READ_WRITE_PERMISSION] ?: false)
        }
    }

    protected fun requestPermission() {
        permissionLauncher.launch(arrayOf(READ_WRITE_PERMISSION))
    }

    protected fun shouldRequestPermission(): Boolean {
        return ContextCompat.checkSelfPermission(context, READ_WRITE_PERMISSION) !=
                PackageManager.PERMISSION_GRANTED
    }

    protected open fun onAnkiRequestPermissionsResult(granted: Boolean) {
        Log.i(TAG, "AnkiPermissions to read/write is granted? $granted")

        if (granted) {
            callbackHandler?.onAnkiRequestPermissionGranted()
            while (callQueue.isNotEmpty()) {
                val action = callQueue.removeFirst()
                lifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                    safelyModifyAnkiDb { action() }
                }
            }
        } else {
            callbackHandler?.onAnkiRequestPermissionDenied()
        }
    }

    /********* Checking Anki's Running & Installed **********/
    protected open suspend fun ensureAnkiDroidIsRunning() {
        withContext(Dispatchers.Main) {
            startAnkiDroid()
        }
    }

    protected fun isApiAvailable(): Boolean {
        return AddContentApi.getAnkiDroidPackageName(context) != null
    }

    protected open fun startAnkiDroid(): Boolean {
        val intent = Intent(Intent.ACTION_MAIN)
        intent.setClassName("com.ichi2.anki", "com.ichi2.anki.IntentHandler")

        return try {
            context.startActivity(intent)
            true
        } catch (e: ActivityNotFoundException) {
            Log.e(TAG, "Anki is not installed, cannot start: $e")
            false
        }
    }

    /********** Our main listening loop **********/
    private fun observeUiEvents() {
        lifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                AnkiSharedEventBus.uiEvents.collect { event ->
                    // Launching on the activity to enable to finish so matter the fragment in the background.
                    fragment.requireActivity().lifecycleScope.launch(Dispatchers.Main) {
                        val appContext = fragment.activity?.applicationContext
                        when (event) {
                            is AnkiSharedEventBus.UiEvent.AnkiAction -> {
                                val result = safelyModifyAnkiDbIfAllowed {
                                    try {
                                        event.action() // action sync must happen on IO thread.
                                    } catch (e: Exception) {
                                        Log.e(
                                            TAG,
                                            "Anki operation yielded an Exception." + e.message
                                        )
                                        Result.failure(Exception("Anki Operation Crashed: " + e.message))
                                    }
                                }

                                result.onSuccess { onAnkiOperationSuccess(appContext) }
                                    .onFailure { e ->
                                        if (e is CancellationException)
                                            onAnkiOperationCancelled(appContext)
                                        else
                                            onAnkiOperationFailed(appContext, e)
                                    }
                            }
                            is AnkiSharedEventBus.UiEvent.AnkiServiceProgress -> {
                                // Handle progress updates for long operations
                                Log.d(TAG, "Progress update: ${event.state.progress}/${event.state.total} - ${event.state.message}")

                                // Forward progress to registered callback
                                onAnkiSyncProgress(appContext, event)
                            }
                            is AnkiSharedEventBus.UiEvent.AnkiServiceStarting -> {
                                onAnkiServiceStarting(appContext, event.serviceDelegate)
                            }
                            is AnkiSharedEventBus.UiEvent.AnkiServiceCancelled -> {
                                onAnkiOperationCancelled(appContext)
                            }
                            is AnkiSharedEventBus.UiEvent.AnkiServiceError -> {
                                onAnkiOperationFailed(appContext, Exception(event.state.message))
                            }
                            is AnkiSharedEventBus.UiEvent.AnkiServiceCompleted -> {
                                onAnkiOperationSuccess(appContext)
                            }
                        }
                    }
                }
            }
        }
    }

    protected suspend fun safelyModifyAnkiDb(ankiDbAction: suspend () -> Result<Unit>): Result<Unit> {
        ensureAnkiDroidIsRunning()
        return withContext(Dispatchers.IO) {
            ankiDbAction()
        }
    }

    protected open suspend fun safelyModifyAnkiDbIfAllowed(ankiDbAction: suspend () -> Result<Unit>): Result<Unit> {
        if (!isApiAvailable()) {
            onAnkiNotInstalled()
            return Result.failure(AnkiOperationsFailures.AnkiFailure_NotInstalled)
        }

        if (shouldRequestPermission()) {
            callQueue.add(ankiDbAction)
            requestPermission()
            return Result.failure(AnkiOperationsFailures.AnkiFailure_Deferred)
        }

        return safelyModifyAnkiDb(ankiDbAction)
    }

    protected fun appContextToast(context: Context?, message: String) {
        if (context == null) {
            return
        }

        Toast.makeText(
            context,
            message,
            Toast.LENGTH_LONG
        ).show()
    }

    /*********** CallBacks ***********/
    protected open fun onAnkiOperationFailed(context: Context?, e: Throwable) {
        if (e is AnkiOperationsFailures.AnkiFailure_Deferred
            || e is AnkiOperationsFailures.AnkiFailure_Off
            || context == null
        )
            return

        callbackHandler?.onAnkiOperationFailed(e)
    }

    protected open fun onAnkiServiceStarting(context: Context?, serviceDelegate: AnkiSyncServiceDelegate) {
        if (context == null) return

        callbackHandler?.onAnkiServiceStarting(serviceDelegate)
    }

    protected open fun onAnkiSyncProgress(context: Context?, event: AnkiSharedEventBus.UiEvent.AnkiServiceProgress) {
        if (context == null) return

        // The service does the notification update

        callbackHandler?.onAnkiSyncProgress(event.state.progress, event.state.total, event.state.message)
    }

    protected open fun onAnkiOperationSuccess(context: Context?) {
        if (context == null) return

        callbackHandler?.onAnkiOperationSuccess()
    }

    protected open fun onAnkiOperationCancelled(context: Context?) {
        if (context == null) return

        callbackHandler?.onAnkiOperationCancelled()
    }

    protected open fun onAnkiNotInstalled() {
    }

    sealed class AnkiOperationsFailures: Throwable() {
        object AnkiFailure_Deferred : AnkiOperationsFailures()
        object AnkiFailure_NotInstalled : AnkiOperationsFailures()
        object AnkiFailure_Off : AnkiOperationsFailures()
    }

    companion object {
        const val TAG = "AnkiDelegate"
    }
}
