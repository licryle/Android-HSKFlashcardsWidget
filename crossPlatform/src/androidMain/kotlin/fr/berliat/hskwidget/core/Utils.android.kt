package fr.berliat.hskwidget.core

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.widget.Toast

import androidx.lifecycle.Observer
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf

import co.touchlab.kermit.Logger

import fr.berliat.hsktextviews.HSKTextSegmenter
import fr.berliat.hskwidget.core.Utils.incrementConsultedWord
import fr.berliat.hskwidget.core.Utils.toast
import fr.berliat.hskwidget.data.dao.AnkiDAO
import fr.berliat.hskwidget.domain.SearchQuery

import fr.berliat.hskwidget.Res
import fr.berliat.hskwidget.cancel
import fr.berliat.hskwidget.copied_to_clipboard
import fr.berliat.hskwidget.dialog_tts_error
import fr.berliat.hskwidget.fix_it
import fr.berliat.hskwidget.speech_failure_toast_chinese_unsupported
import fr.berliat.hskwidget.speech_failure_toast_init
import fr.berliat.hskwidget.speech_failure_toast_muted
import fr.berliat.hskwidget.speech_failure_toast_unknown

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString

fun Context.findActivity(): Activity = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> throw Exception("No activity attached to context")
}

@SuppressLint("StaticFieldLeak")
actual object ExpectedUtils {
    private var _context: Context? = null
    val context
        get() = _context!!

    // Initialize once from Compose or Activity
    fun init(context: Context) {
        _context = context
    }

    actual fun openLink(url: String) {
        val intent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse(url)
        ).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        context.startActivity(intent)
    }

    actual fun sendEmail(email: String, subject: String, body: String) : Boolean {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:") // use only "mailto:", don't put address here
            putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
        }

        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
        } else {
            try {
                context.startActivity(Intent.createChooser(intent, "Send email with..."))
            } catch (_: Exception) {
                return false
            }
        }

        return true
    }

    actual fun logAnalyticsScreenView(screen: String) {
    }

    actual fun logAnalyticsEvent(event: Utils.ANALYTICS_EVENTS,
                                 params: Map<String, String>) {
    }

    actual fun logAnalyticsWidgetAction(event: Utils.ANALYTICS_EVENTS, widgetId: Int) {
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

    actual fun getAnkiDAO(): AnkiDAO {
        return AnkiDAO(context)
    }

    actual fun getHSKSegmenter() : HSKTextSegmenter {
        return JiebaHSKTextSegmenter()
    }

    actual fun copyToClipBoard(s: String) {
        // https://stackoverflow.com/a/28780585/3059536
        val context = context

        val clipboard =
            context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Copied Text", s)
        clipboard.setPrimaryClip(clip)

        toast(Res.string.copied_to_clipboard, listOf(s))

        logAnalyticsEvent(Utils.ANALYTICS_EVENTS.WIDGET_COPY_WORD)

        incrementConsultedWord(s)
    }

    actual fun playWordInBackground(word: String) {
        val context = context
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

                        CoroutineScope(Dispatchers.IO).launch {
                            val errString = getString(errStringId)
                            val titleString =getString(Res.string.dialog_tts_error)
                            val yesButton = getString(Res.string.fix_it)
                            val noButton = getString(Res.string.cancel)

                            if (errRemedyIntent == null) {
                                toast(errStringId)
                            } else {
                                withContext(Dispatchers.Main) {
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

    actual fun toast(s: String) {
        CoroutineScope(Dispatchers.Main).launch {
            Toast.makeText(context, s, Toast.LENGTH_LONG).show()
        }
    }

    actual fun openAppForSearchQuery(query: SearchQuery) {
        val context = context
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

    private const val TAG = "Utils"
    const val INTENT_SEARCH_WORD = "INTENT_SEARCH_WORD"
}
