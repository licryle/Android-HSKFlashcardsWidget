package fr.berliat.hskwidget.ui.utils

import android.content.Context
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import fr.berliat.ankidroidhelper.AnkiDelegate
import fr.berliat.hskwidget.R
import fr.berliat.hskwidget.core.HSKAppServices
import fr.berliat.hskwidget.data.store.AnkiStore
import fr.berliat.hskwidget.data.store.OldAppPreferencesStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HSKAnkiDelegate(val fragment: Fragment, handler: HandlerInterface? = null)  : AnkiDelegate(fragment, handler) {
    private val context = fragment.requireContext()
    private val appConfig = OldAppPreferencesStore(context)
    private lateinit var ankiStore : AnkiStore

    init {
        fragment.lifecycleScope.launch(Dispatchers.IO) {
            ankiStore = HSKAppServices.ankiStore
        }
    }

    override fun onAnkiRequestPermissionsResult(granted: Boolean) {
        appConfig.ankiSaveNotes = granted
        super.onAnkiRequestPermissionsResult(granted)
        if (!granted) {
            Toast.makeText(context, R.string.anki_permission_denied, Toast.LENGTH_LONG).show()
        }
    }

    override fun startAnkiDroid(): Boolean {
        Toast.makeText(context, R.string.anki_must_start, Toast.LENGTH_LONG).show()

        val result = super.startAnkiDroid()
        if (!result) {
            Toast.makeText(context, R.string.anki_not_installed, Toast.LENGTH_LONG).show()
        }

        return result
    }

    override suspend fun safelyModifyAnkiDbIfAllowed(ankiDbAction: suspend () -> Result<Unit>): Result<Unit>
            = withContext(Dispatchers.IO) {
        if (!appConfig.ankiSaveNotes)
            return@withContext Result.failure(AnkiOperationsFailures.AnkiFailure_Off)

        return@withContext super.safelyModifyAnkiDbIfAllowed(ankiDbAction)
    }

    override suspend fun ensureAnkiDroidIsRunning() = withContext(Dispatchers.IO) {
        if (!ankiStore.isStoreReady()) {
            super.ensureAnkiDroidIsRunning()
        }
    }

    /*********** CallBacks ***********/
    override fun onAnkiOperationFailed(context: Context?, e: Throwable) {
        if (!(e is AnkiOperationsFailures.AnkiFailure_Deferred
            || e is AnkiOperationsFailures.AnkiFailure_Off
            || context == null
        )) {
            var message = context.getString(R.string.anki_operation_failed)
            message = message.format(e.message)
            appContextToast(context, message)
        }

        super.onAnkiOperationFailed(context, e)
    }

    override fun onAnkiOperationSuccess(context: Context?) {
        context?.let {
            appContextToast(context, context.getString(R.string.anki_operation_success))
        }

        super.onAnkiOperationSuccess(context)
    }

    override fun onAnkiOperationCancelled(context: Context?) {
        context?.let {
            appContextToast(context, context.getString(R.string.anki_operation_cancelled))
        }

        super.onAnkiOperationCancelled(context)
    }

    override fun onAnkiNotInstalled() {
        appContextToast(context, context.getString(R.string.anki_not_installed))
        appConfig.ankiSaveNotes = false

        super.onAnkiNotInstalled()
    }
}
