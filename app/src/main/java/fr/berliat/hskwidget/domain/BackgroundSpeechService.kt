package fr.berliat.hskwidget.domain

import android.content.Context
import android.os.SystemClock.sleep
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.util.Locale

class BackgroundSpeechService(context: Context, workerParams: WorkerParameters)
    : Worker(context, workerParams), TextToSpeech.OnInitListener {
    private val textToSpeech = TextToSpeech(context, this)
    private var initStatus: Int? = null

    override fun doWork(): Result {
        while (initStatus == null) { sleep(50); }

        if (initStatus != TextToSpeech.SUCCESS) {
            Log.e("TTS", "Initialization Failed!")
            return Result.failure()
        }

        val result = textToSpeech.setLanguage(Locale.SIMPLIFIED_CHINESE)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            Log.e("TTS","Simplified_chinese not supported on this phone.")
            return Result.failure()
        }

        textToSpeech.speak(inputData.getString("word"), TextToSpeech.QUEUE_FLUSH, null,"")

        return Result.success()
    }

    override fun onInit(status: Int) {
        initStatus = status
    }
}