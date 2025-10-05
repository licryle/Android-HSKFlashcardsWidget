package fr.berliat.hskwidget.ui

import android.content.Context
import androidx.fragment.app.FragmentActivity

import fr.berliat.ankidroidhelper.AnkiDelegate
import fr.berliat.hskwidget.core.Utils
import fr.berliat.hskwidget.core.HSKAppServices
import fr.berliat.hskwidget.data.store.AnkiStore
import fr.berliat.hskwidget.data.store.AppPreferencesStore

import fr.berliat.hskwidget.Res
import fr.berliat.hskwidget.anki_must_start
import fr.berliat.hskwidget.anki_not_installed
import fr.berliat.hskwidget.anki_operation_cancelled
import fr.berliat.hskwidget.anki_operation_failed
import fr.berliat.hskwidget.anki_operation_success
import fr.berliat.hskwidget.anki_permission_denied

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

import org.jetbrains.compose.resources.getString

class HSKAnkiDelegate(val activity: FragmentActivity,
                      handler: HandlerInterface? = null,
                      var appConfig: AppPreferencesStore? = HSKAppServices.appPreferences,
                      var ankiStore: AnkiStore? = HSKAppServices.ankiStore)  : AnkiDelegate(activity, handler) {
    override fun onAnkiRequestPermissionsResult(granted: Boolean) {
        appConfig?.ankiSaveNotes?.value = granted
        super.onAnkiRequestPermissionsResult(granted)
        if (!granted) {
            Utils.toast(Res.string.anki_permission_denied)
        }
    }

    override fun startAnkiDroid(): Boolean {
        Utils.toast(Res.string.anki_must_start)

        val result = super.startAnkiDroid()
        if (!result) {
            Utils.toast(Res.string.anki_not_installed)
        }

        return result
    }

    override suspend fun safelyModifyAnkiDbIfAllowed(ankiDbAction: suspend () -> Result<Unit>): Result<Unit>
            = withContext(Dispatchers.IO) {
        if (!(appConfig?.ankiSaveNotes?.value ?: false))
            return@withContext Result.failure(AnkiOperationsFailures.AnkiFailure_Off)

        return@withContext super.safelyModifyAnkiDbIfAllowed(ankiDbAction)
    }

    override suspend fun ensureAnkiDroidIsRunning() = withContext(Dispatchers.IO) {
        if (!(ankiStore?.isStoreReady() ?: false)) {
            super.ensureAnkiDroidIsRunning()
        }
    }

    /*********** CallBacks ***********/
    override fun onAnkiOperationFailed(context: Context?, e: Throwable) {
        if (!(e is AnkiOperationsFailures.AnkiFailure_Deferred
            || e is AnkiOperationsFailures.AnkiFailure_Off
            || context == null
        )) {
            // Todo remove runBlocking
            var message = runBlocking { getString(Res.string.anki_operation_failed) }
            message = message.format(e.message)
            Utils.toast(message)
        }

        super.onAnkiOperationFailed(context, e)
    }

    override fun onAnkiOperationSuccess(context: Context?) {
        Utils.toast(Res.string.anki_operation_success)

        super.onAnkiOperationSuccess(context)
    }

    override fun onAnkiOperationCancelled(context: Context?) {
        Utils.toast(Res.string.anki_operation_cancelled)

        super.onAnkiOperationCancelled(context)
    }

    override fun onAnkiNotInstalled() {
        Utils.toast(Res.string.anki_not_installed)
        appConfig?.ankiSaveNotes?.value = false

        super.onAnkiNotInstalled()
    }
}