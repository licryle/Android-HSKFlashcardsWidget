package fr.berliat.hskwidget.core

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.Context
import android.content.ContextWrapper
import android.media.AudioManager
import android.net.Uri
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.Toast

import co.touchlab.kermit.Logger

import fr.berliat.hsktextviews.HSKTextSegmenter
import fr.berliat.hskwidget.core.Utils.incrementConsultedWord
import fr.berliat.hskwidget.core.Utils.toast
import fr.berliat.hskwidget.data.dao.AnkiDAO
import fr.berliat.hskwidget.domain.SearchQuery
import fr.berliat.hskwidget.Res
import fr.berliat.hskwidget.cancel
import fr.berliat.hskwidget.copied_to_clipboard
import fr.berliat.hskwidget.core.Logging.logAnalyticsError
import fr.berliat.hskwidget.core.Logging.logAnalyticsEvent
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

import java.util.Locale

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

        logAnalyticsEvent(Logging.ANALYTICS_EVENTS.WIDGET_COPY_WORD)

        incrementConsultedWord(s)
    }

    private fun isMuted() : Boolean {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val musicVolume: Int = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        return musicVolume == 0
    }

    actual fun playWordInBackground(word: String) {
        data class SpeechError(val errStringId: StringResource, var errRemedyIntent: String? = null)
        val TAG = TAG

        var err : SpeechError? = null

        if (isMuted()) {
            err = SpeechError(Res.string.speech_failure_toast_muted)
        } else {
            var tts: TextToSpeech? = null

            try {
                tts = TextToSpeech(context) { status ->
                    if (status != TextToSpeech.SUCCESS) {
                        err = SpeechError(
                            Res.string.speech_failure_toast_init,
                            Settings.ACTION_ACCESSIBILITY_SETTINGS
                        )
                    } else {
                        val result = tts?.setLanguage(Locale.SIMPLIFIED_CHINESE)

                        Log.i(TAG, "Setting language to play $word out loud.")
                        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                            err = SpeechError(Res.string.speech_failure_toast_chinese_unsupported)
                            Log.e(TAG, "Simplified_chinese not supported on this phone.")

                            val installIntent = Intent()
                            installIntent.action = TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA
                            installIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            context.startActivity(installIntent)
                        } else {
                            Log.i(TAG, "Playing $word out loud.")
                            tts?.speak(
                                word,
                                TextToSpeech.QUEUE_FLUSH,
                                null,
                                "tts-${word.hashCode()}"
                            )
                        }
                    }
                }
            } catch (_: Exception) {
                err = SpeechError(Res.string.speech_failure_toast_unknown)
            }
        }

        err?.let {
            CoroutineScope(Dispatchers.IO).launch {
                val errString = getString(err.errStringId)
                val titleString =getString(Res.string.dialog_tts_error)
                val yesButton = getString(Res.string.fix_it)
                val noButton = getString(Res.string.cancel)

                if (err.errRemedyIntent == null) {
                    toast(err.errStringId)
                } else {
                    withContext(Dispatchers.Main) {
                        AlertDialog.Builder(context)
                            .setTitle(titleString)
                            .setMessage(errString)
                            .setPositiveButton(yesButton) { _, _ ->
                                val intent = Intent(err.errRemedyIntent)
                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                context.startActivity(intent)
                            }
                            .setNegativeButton(noButton, null)
                            .show()
                    }
                }

                logAnalyticsError("SPEECH", getString(err.errStringId), "")
            }
        }

        incrementConsultedWord(word)

        logAnalyticsEvent(Logging.ANALYTICS_EVENTS.WIDGET_PLAY_WORD)
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
            Logging.logAnalyticsEvent(Logging.ANALYTICS_EVENTS.WIDGET_OPEN_DICTIONARY)
            context.startActivity(launchIntent)
        } else {
            // fallback: app has no launch intent?
            Logger.e(tag = TAG, messageString = "No launch intent found for ${context.packageName}")
        }
    }

    private const val TAG = "Utils"
    const val INTENT_SEARCH_WORD = "INTENT_SEARCH_WORD"
}
