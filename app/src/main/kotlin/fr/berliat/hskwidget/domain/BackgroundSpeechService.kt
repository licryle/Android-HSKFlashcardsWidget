package fr.berliat.hskwidget.domain

import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.media.AudioManager
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import androidx.work.Data
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.util.Locale
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class BackgroundSpeechService(val context: Context, workerParams: WorkerParameters) :
    Worker(context, workerParams) {
    private val word = inputData.getString("word")

    companion object {
        const val TAG = "BackgroundSpeechService"
        const val FAILURE_UNKNOWN = "FAILURE_UNKNOWN"
        const val FAILURE_REASON = "FAILURE_REASON"
        const val FAILURE_MUTED = "FAILURE_MUTED"
        const val FAILURE_INIT_FAILED = "FAILURE_INIT_FAILED"
        const val FAILURE_LANG_UNSUPPORTED = "FAILURE_LANG_UNSUPPORTED"
    }

    override fun doWork(): Result {
        Log.i(TAG, "Readying to play  $word out loud.")
        if (isMuted()) {
            Log.i(TAG, "But volume is muted. Aborting.")
            return buildErrorResult(FAILURE_MUTED)
        }

        var tts: TextToSpeech?
        return try {
            val latch = CountDownLatch(1)
            var initStatus: Int? = null
            tts = TextToSpeech(context) { status ->
                initStatus = status
                latch.countDown()
            }

            if (!latch.await(5, TimeUnit.SECONDS)) {
                Log.e(TAG, "TTS init timed out")
                return buildErrorResult(FAILURE_INIT_FAILED)
            }

            if (initStatus != TextToSpeech.SUCCESS) {
                Log.e(TAG, "Initialization Failed!")
                return buildErrorResult(FAILURE_INIT_FAILED)
            }

            Log.i(TAG, "Setting language to play  $word out loud.")
            val result = tts.setLanguage(Locale.SIMPLIFIED_CHINESE)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e(TAG, "Simplified_chinese not supported on this phone.")

                val installIntent = Intent()
                installIntent.action = TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA
                installIntent.flags = FLAG_ACTIVITY_NEW_TASK
                context.startActivity(installIntent)

                return buildErrorResult(FAILURE_LANG_UNSUPPORTED)
            }

            Log.i(TAG, "Starting to play  $word out loud.")
            tts.speak(word, TextToSpeech.QUEUE_FLUSH, null, "")

            // Wait for speech completion
            val speechLatch = CountDownLatch(1)
            tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}
                override fun onDone(utteranceId: String?) {
                    speechLatch.countDown()
                    tts.shutdown()
                }
                override fun onError(utteranceId: String?) {
                    speechLatch.countDown()
                    tts.shutdown()
                }
            })
            Log.i(TAG, "Finishing to play  $word out loud.")

            return Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected failure", e)
            buildErrorResult(FAILURE_INIT_FAILED)
        }
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