package fr.berliat.hskwidget.domain

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioManager
import android.os.SystemClock.sleep
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.work.Data
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.util.Locale

class BackgroundSpeechService(val context: Context, workerParams: WorkerParameters)
    : Worker(context, workerParams), TextToSpeech.OnInitListener {
    private val textToSpeech = TextToSpeech(context, this)
    private var initStatus: Int? = null
    private val word = inputData.getString("word")

    companion object {
        const val FAILURE_REASON = "FAILURE_REASON"
        const val FAILURE_MUTED = "FAILURE_MUTED"
    }

    @SuppressLint("RestrictedApi")
    override fun doWork(): Result {
        Log.i("BackgroundSpeechService", "Starting to play  ${word} out loud.")
        if (isMuted()) {
            Log.i("BackgroundSpeechService", "But volume is muted. Aborting.")
            return Result.failure(Data(mapOf(FAILURE_REASON to FAILURE_MUTED)))
        }

        while (initStatus == null) { sleep(10); }

        if (initStatus != TextToSpeech.SUCCESS) {
            Log.e("BackgroundSpeechService", "Initialization Failed!")
            return Result.failure()
        }

        val result = textToSpeech.setLanguage(Locale.SIMPLIFIED_CHINESE)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            Log.e("BackgroundSpeechService","Simplified_chinese not supported on this phone.")
            return Result.failure()
        }

        textToSpeech.speak(word, TextToSpeech.QUEUE_FLUSH, null,"")
        Log.i("BackgroundSpeechService", "Finishing to play  ${word} out loud.")

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
}