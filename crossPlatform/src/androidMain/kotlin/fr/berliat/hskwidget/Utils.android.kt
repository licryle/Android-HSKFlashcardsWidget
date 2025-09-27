package fr.berliat.hskwidget

import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.Context
import android.net.Uri
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.widget.Toast

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.Observer
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf

import co.touchlab.kermit.Logger

import fr.berliat.hsktextviews.HSKTextSegmenter
import fr.berliat.hskwidget.Utils.incrementConsultedWord
import fr.berliat.hskwidget.core.BackgroundSpeechService
import fr.berliat.hskwidget.core.JiebaHSKTextSegmenter
import fr.berliat.hskwidget.data.dao.AnkiDAO
import fr.berliat.hskwidget.domain.SearchQuery

import hskflashcardswidget.crossplatform.generated.resources.Res
import hskflashcardswidget.crossplatform.generated.resources.cancel
import hskflashcardswidget.crossplatform.generated.resources.copied_to_clipboard
import hskflashcardswidget.crossplatform.generated.resources.dialog_tts_error
import hskflashcardswidget.crossplatform.generated.resources.fix_it
import hskflashcardswidget.crossplatform.generated.resources.speech_failure_toast_chinese_unsupported
import hskflashcardswidget.crossplatform.generated.resources.speech_failure_toast_init
import hskflashcardswidget.crossplatform.generated.resources.speech_failure_toast_muted
import hskflashcardswidget.crossplatform.generated.resources.speech_failure_toast_unknown

import kotlinx.coroutines.runBlocking

import okio.Path.Companion.toPath

import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString

actual object ExpectedUtils {
    private var _contextProvider: (() -> Context)? = null
    fun context() = _contextProvider!!.invoke()


    // Initialize once from Compose or Activity
    fun init(contextProvider: () -> Context) {
        this._contextProvider = contextProvider
    }

    actual fun openLink(url: String) {
        val intent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse(url)
        ).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        context().startActivity(intent)
    }

    actual fun sendEmail(email: String, subject: String, body: String) : Boolean {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:") // use only "mailto:", don't put address here
            putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
        }

        if (intent.resolveActivity(context().packageManager) != null) {
            context().startActivity(intent)
        } else {
            try {
                context().startActivity(Intent.createChooser(intent, "Send email with..."))
            } catch (_: Exception) {
                return false
            }
        }

        return true
    }

    actual fun getPlatform(): String {
        return "Android ${getAppVersion()}"
    }

    // androidMain
    actual fun getAppVersion(): String = "Not supported yet"

    actual fun logAnalyticsScreenView(screen: String) {
    }

    actual fun logAnalyticsEvent(event: Utils.ANALYTICS_EVENTS,
                                 params: Map<String, String>) {
    }

    actual fun logAnalyticsError(module: String, error: String, details: String) {
        logAnalyticsEvent(
            Utils.ANALYTICS_EVENTS.ERROR,
            mapOf(
                "MODULE" to module,
                "ERROR_ID" to error,
                "DETAILS" to details
            )
        )
    }

    actual fun getDataStore(file: String): DataStore<Preferences> {
        return PreferenceDataStoreFactory.createWithPath(
            produceFile = { context().filesDir.resolve(file).absolutePath.toPath() }
        )
    }

    actual fun getAnkiDAO(): AnkiDAO {
        return AnkiDAO(context())
    }

    actual fun getHSKSegmenter() : HSKTextSegmenter {
        return JiebaHSKTextSegmenter()
    }

    actual fun copyToClipBoard(s: String) {
        // https://stackoverflow.com/a/28780585/3059536
        val context = context()

        val clipboard =
            context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Copied Text", s)
        clipboard.setPrimaryClip(clip)

        // TODO clean the runBlocking
        val str = runBlocking { getString(Res.string.copied_to_clipboard) }

        Toast.makeText(
            context,
            String.format(str, s),
            Toast.LENGTH_SHORT
        ).show()

        logAnalyticsEvent(Utils.ANALYTICS_EVENTS.WIDGET_COPY_WORD)

        incrementConsultedWord(s)
    }

    actual fun playWordInBackground(word: String) {
        val context = context()
        val speechRequest = OneTimeWorkRequestBuilder<BackgroundSpeechService>()
            .setInputData(workDataOf(Pair("word", word)))
            .build()

        val workMgr = WorkManager.getInstance(context)
        val observer = object : Observer<WorkInfo?> {
            override fun onChanged(value: WorkInfo?) {
                if (value == null) {
                    // Handle the case where workInfo is null
                    return
                }

                if (value.state == WorkInfo.State.SUCCEEDED
                    || value.state == WorkInfo.State.FAILED
                ) {

                    val errStringId: StringResource
                    var errRemedyIntent: String? = null
                    if (value.state == WorkInfo.State.FAILED) {
                        var errId =
                            value.outputData.getString(BackgroundSpeechService.FAILURE_REASON)
                        when (errId) {
                            BackgroundSpeechService.FAILURE_MUTED
                                -> errStringId = Res.string.speech_failure_toast_muted

                            BackgroundSpeechService.FAILURE_INIT_FAILED -> {
                                errStringId = Res.string.speech_failure_toast_init
                                errRemedyIntent = Settings.ACTION_ACCESSIBILITY_SETTINGS
                            }

                            BackgroundSpeechService.FAILURE_LANG_UNSUPPORTED -> {
                                errStringId = Res.string.speech_failure_toast_chinese_unsupported
                                errRemedyIntent = TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA
                            }

                            else -> {
                                errStringId = Res.string.speech_failure_toast_unknown
                                errId = BackgroundSpeechService.FAILURE_UNKNOWN
                            }
                        }

                        logAnalyticsError("SPEECH", errId, "")

                        // TODO clean the runBlocking
                        val errString = runBlocking { getString(errStringId) }
                        val titleString = runBlocking { getString(Res.string.dialog_tts_error) }
                        val yesButton = runBlocking { getString(Res.string.fix_it) }
                        val noButton = runBlocking { getString(Res.string.cancel) }

                        if (errRemedyIntent == null) {
                            Toast.makeText(
                                context, errString,
                                Toast.LENGTH_LONG
                            ).show()
                        } else {
                            AlertDialog.Builder(context)
                                .setTitle(titleString)
                                .setMessage(errString)
                                .setPositiveButton(yesButton) { _, _ ->
                                    val intent = Intent(errRemedyIntent)
                                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    context.startActivity(intent)
                                }
                                .setNegativeButton(noButton, null)
                                .show()
                        }
                    }

                    workMgr.getWorkInfoByIdLiveData(speechRequest.id)
                        .removeObserver(this)
                }
            }
        }

        workMgr.getWorkInfoByIdLiveData(speechRequest.id).observeForever(observer)

        workMgr.enqueue(speechRequest)

        incrementConsultedWord(word)

        logAnalyticsEvent(Utils.ANALYTICS_EVENTS.WIDGET_PLAY_WORD)
    }

    actual fun toast(stringRes: StringResource, args: List<String>) {
        val s = runBlocking { getString(stringRes) } // Todo change to coroutine
        s.format(args)

        Toast.makeText(context(), s.format(args), Toast.LENGTH_LONG).show()
    }

    actual fun openAppForSearchQuery(query: SearchQuery) {
        val context = context()
        val pm = context.packageManager
        val launchIntent = pm.getLaunchIntentForPackage(context.packageName)?.apply {
            putExtra(INTENT_SEARCH_WORD, query.toString())
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }

        if (launchIntent != null) {
            Utils.logAnalyticsEvent(Utils.ANALYTICS_EVENTS.WIDGET_OPEN_DICTIONARY)
            context.startActivity(launchIntent)
        } else {
            // fallback: app has no launch intent?
            Logger.e(tag = TAG, messageString = "No launch intent found for ${context.packageName}")
        }
    }

    const val TAG = "Utils"
    const val INTENT_SEARCH_WORD = "INTENT_SEARCH_WORD"
}