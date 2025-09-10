package fr.berliat.hskwidget.ui.utils

import android.widget.Toast
import androidx.fragment.app.Fragment
import fr.berliat.ankidroidhelper.AnkiDelegate
import fr.berliat.hskwidget.R
import fr.berliat.hskwidget.data.store.AnkiStore
import fr.berliat.hskwidget.data.store.AppPreferencesStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class HSKAnkiDelegate(val fragment: Fragment, handler: HandlerInterface? = null)  : AnkiDelegate(fragment, handler) {
    private val context = fragment.requireContext()
    private val appConfig = AppPreferencesStore(context)
    private val ankiStore = AnkiStore(context)

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

    override suspend fun safelyModifyAnkiDbIfAllowed(ankiDbAction: suspend () -> Result<Unit>): Result<Unit>?
            = withContext(Dispatchers.IO) {
        if (!appConfig.ankiSaveNotes)
            return@withContext Result.failure(AnkiOperationsFailures.CustomError("AnkiBackupNotActive"))

        return@withContext super.safelyModifyAnkiDbIfAllowed(ankiDbAction)
    }

    override suspend fun ensureAnkiDroidIsRunning() = withContext(Dispatchers.IO) {
        if (!ankiStore.isStoreReady()) {
            super.ensureAnkiDroidIsRunning()
        }
    }

    /*********** CallBacks ***********/
    override fun onAnkiOperationFailed(e: Throwable) {
        if (!(e is AnkiOperationsFailures.AnkiFailure_NoPermission
                    || e is AnkiOperationsFailures.AnkiFailure_NotInstalled)) {
            var message = context.getString(R.string.anki_operation_failed)
            message = message.format(e.message)
            appContextToast(message)
        }

        super.onAnkiOperationFailed(e)
    }

    override fun onAnkiOperationSuccess() {
        appContextToast(context.getString(R.string.anki_operation_success))

        super.onAnkiOperationSuccess()
    }

    override fun onAnkiOperationCancelled() {
        appContextToast(context.getString(R.string.anki_operation_cancelled))

        super.onAnkiOperationCancelled()
    }

    override fun onAnkiNotInstalled() {
        appContextToast(context.getString(R.string.anki_not_installed))
        appConfig.ankiSaveNotes = false

        super.onAnkiNotInstalled()
    }
}
