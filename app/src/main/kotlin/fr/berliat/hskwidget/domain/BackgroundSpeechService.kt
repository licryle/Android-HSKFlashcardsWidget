package fr.berliat.hskwidget.domain

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.media.AudioManager
import android.os.SystemClock.sleep
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.work.Data
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.util.Locale


class BackgroundSpeechService(val context: Context, workerParams: WorkerParameters) :
    Worker(context, workerParams), TextToSpeech.OnInitListener {
    private val textToSpeech = TextToSpeech(context, this)
    private var initStatus: Int? = null
    private val word = inputData.getString("word")

    companion object {
        const val TAG = "BackgroundSpeechService"
        const val FAILURE_UNKNOWN = "FAILURE_UNKNOWN"
        const val FAILURE_REASON = "FAILURE_REASON"
        const val FAILURE_MUTED = "FAILURE_MUTED"
        const val FAILURE_INIT_FAILED = "FAILURE_INIT_FAILED"
        const val FAILURE_LANG_UNSUPPORTED = "FAILURE_LANG_UNSUPPORTED"
    }

    @SuppressLint("RestrictedApi")
    override fun doWork(): Result {
        Log.i(TAG, "Readying to play  ${word} out loud.")
        if (isMuted()) {
            Log.i(TAG, "But volume is muted. Aborting.")
            return buildErrorResult(FAILURE_MUTED)
        }

        while (initStatus == null) {
            Log.i(TAG, "Not (yet) ready to play  ${word} out loud.")
            sleep(10)
        }

        if (initStatus != TextToSpeech.SUCCESS) {
            Log.e(TAG, "Initialization Failed!")
            return buildErrorResult(FAILURE_INIT_FAILED)
        }

        Log.i(TAG, "Setting language to play  ${word} out loud.")
        val result = textToSpeech.setLanguage(Locale.SIMPLIFIED_CHINESE)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            Log.e(TAG, "Simplified_chinese not supported on this phone.")

            val installIntent = Intent()
            installIntent.action = TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA
            installIntent.flags = FLAG_ACTIVITY_NEW_TASK
            context.startActivity(installIntent)

            return buildErrorResult(FAILURE_LANG_UNSUPPORTED)
        }

        Log.i(TAG, "Starting to play  ${word} out loud.")
        textToSpeech.speak(word, TextToSpeech.QUEUE_FLUSH, null, "")
        Log.i(TAG, "Finishing to play  ${word} out loud.")

        return Result.success()
    }

    override fun onInit(status: Int) {
        initStatus = status
    }

    private fun isMuted() : Boolean {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val musicVolume: Int = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        return musicVolume == 0
    }

    private fun buildErrorResult(reason: String) : Result {
        return Result.failure(Data.Builder().putString(FAILURE_REASON, reason).build())
    }
}