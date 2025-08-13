package fr.berliat.hskwidget.ui.utils

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
import fr.berliat.hskwidget.R
import fr.berliat.hskwidget.data.repo.WordListRepository
import fr.berliat.hskwidget.data.repo.WordListRepository.SharedEventBus as AnkiEventBus
import fr.berliat.hskwidget.data.store.AnkiStore
import fr.berliat.hskwidget.data.store.AppPreferencesStore
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
 * fragment thread. This Helper handles it all.
 *
 * Here's a simplistic example:
 *     override fun onCreate(savedInstanceState: Bundle?) {
 *         super.onCreate(savedInstanceState)
 *
 *         ankiDelegate = AnkiDelegate(this)
 *         ankiDelegate.workListRepo.delegateToAnki(ankiDelegate.insertWordToList(list, word))
 *     }
 *
 * If you use a viewModel, make sure to use the same workListRepo from your AnkiDelegateHelper.
 * In other words, don't instantiate WordListRepository yourself. Take it from the Helper, always.
 *
 * Beware of execution patterns, as the callbacks can mean Anki calls executing after whatever
 * element you change/delete.
 */
class AnkiDelegate(
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
    private val appConfig = AppPreferencesStore(context)
    private val ankiStore = AnkiStore(context)
    private val callQueue: ArrayDeque<suspend () -> Result<Unit>> = ArrayDeque()
    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>

    val wordListRepo = WordListRepository(context) // Use it for setting up the viewModels

    init {
        initPermissionHandling { isGranted -> onAnkiRequestPermissionsResult(isGranted) }
        observeUiEvents()
    }

    /********** Anki Permissions ************/
    private fun initPermissionHandling(callback: (Boolean) -> Unit) {
        permissionLauncher = fragment.registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()) { result ->
            callback(result[READ_WRITE_PERMISSION] ?: false)
        }
    }

    private fun requestPermission() {
        permissionLauncher.launch(arrayOf(READ_WRITE_PERMISSION))
    }

    private fun shouldRequestPermission(): Boolean {
        return ContextCompat.checkSelfPermission(context, READ_WRITE_PERMISSION) !=
                PackageManager.PERMISSION_GRANTED
    }

    private fun onAnkiRequestPermissionsResult(granted: Boolean) {
        Log.i(TAG, "AnkiPermissions to read/write is granted? $granted")
        appConfig.ankiSaveNotes = granted
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
            Toast.makeText(context, R.string.anki_permission_denied, Toast.LENGTH_LONG).show()
        }
    }

    /********* Checking Anki's Running & Installed **********/
    private suspend fun ensureAnkiDroidIsRunning() {
        if (!ankiStore.isStoreReady()) {
            withContext(Dispatchers.Main) {
                startAnkiDroid()
            }
        }
    }

    private fun isApiAvailable(): Boolean {
        return AddContentApi.getAnkiDroidPackageName(context) != null
    }

    private fun startAnkiDroid(): Boolean {
        val intent = Intent(Intent.ACTION_MAIN)
        intent.setClassName("com.ichi2.anki", "com.ichi2.anki.IntentHandler")

        return try {
            Toast.makeText(context, R.string.anki_must_start, Toast.LENGTH_LONG).show()
            context.startActivity(intent)
            true
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, R.string.anki_not_installed, Toast.LENGTH_LONG).show()
            Log.e(TAG, context.getString(R.string.anki_not_installed), e)
            false
        }
    }

    /********** Our main listening loop **********/
    private fun observeUiEvents() {
        lifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                AnkiEventBus.uiEvents.collect { event ->
                    // Launching on the activity to enable to finish so matter the fragment in the background.
                    fragment.requireActivity().lifecycleScope.launch(Dispatchers.Main) {
                        val appContext = fragment.activity?.applicationContext
                        when (event) {
                            is AnkiEventBus.UiEvent.TriggerAnkiSync -> {
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
                            is AnkiEventBus.UiEvent.ProgressUpdate -> {
                                // Handle progress updates for long operations
                                Log.d(TAG, "Progress update: ${event.state.progress}/${event.state.total} - ${event.state.message}")
                                
                                // Forward progress to registered callback
                                onAnkiSyncProgress(appContext, event)
                            }
                            is AnkiEventBus.UiEvent.ServiceStarting -> {
                                onAnkiServiceStarting(appContext, event.serviceDelegate)
                            }
                        }
                    }
                }
            }
        }
    }

    private suspend fun safelyModifyAnkiDb(ankiDbAction: suspend () -> Result<Unit>): Result<Unit> {
        ensureAnkiDroidIsRunning()
        return withContext(Dispatchers.IO) {
            ankiDbAction()
        }
    }

    private suspend fun safelyModifyAnkiDbIfAllowed(ankiDbAction: suspend () -> Result<Unit>): Result<Unit> {
        if (!appConfig.ankiSaveNotes) return Result.failure(AnkiOperationsFailures.AnkiFailure_Off)

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

    private fun appContextToast(context: Context?, message: String) {
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
    private fun onAnkiOperationFailed(context: Context?, e: Throwable) {
        if (e is AnkiOperationsFailures.AnkiFailure_Deferred
            || e is AnkiOperationsFailures.AnkiFailure_Off
            || context == null
        )
            return

        var message = context.getString(R.string.anki_operation_failed)
        message = message.format(e.message)
        appContextToast(context, message)

        callbackHandler?.onAnkiOperationFailed(e)
    }

    private fun onAnkiServiceStarting(context: Context?, serviceDelegate: AnkiSyncServiceDelegate) {
        if (context == null) return

        callbackHandler?.onAnkiServiceStarting(serviceDelegate)
    }

    private fun onAnkiSyncProgress(context: Context?, event: AnkiEventBus.UiEvent.ProgressUpdate) {
        if (context == null) return

        // The service does the notification update

        callbackHandler?.onAnkiSyncProgress(event.state.progress, event.state.total, event.state.message)
    }

    private fun onAnkiOperationSuccess(context: Context?) {
        if (context == null) return

        appContextToast(context, context.getString(R.string.anki_operation_success))

        callbackHandler?.onAnkiOperationSuccess()
    }

    private fun onAnkiOperationCancelled(context: Context?) {
        if (context == null) return

        appContextToast(context, context.getString(R.string.anki_operation_cancelled))

        callbackHandler?.onAnkiOperationCancelled()
    }

    private fun onAnkiNotInstalled() {
        Toast.makeText(
            context,
            context.getString(R.string.anki_not_installed),
            Toast.LENGTH_LONG
        ).show()
        // App was uninstalled, turning it off

        appConfig.ankiSaveNotes = false
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
