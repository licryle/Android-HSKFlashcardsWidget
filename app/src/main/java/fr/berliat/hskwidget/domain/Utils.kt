package fr.berliat.hskwidget.domain

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.Observer
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import fr.berliat.hskwidget.R
import fr.berliat.hskwidget.data.model.ChineseWord
import java.util.Locale

class Utils {
    class DummyWorker(context: Context, workerParams: WorkerParameters)
        : Worker(context, workerParams) {
        override fun doWork(): Result {
            return Result.success()
        }
    }
    companion object {
        fun getDefaultWord(context: Context): ChineseWord {
            return ChineseWord(
                context.getString(R.string.widget_default_chinese),
                "",
                mapOf(Locale.ENGLISH to context.getString(R.string.widget_default_english)),
                ChineseWord.HSK_Level.HSK1,
                ChineseWord.Pinyins(context.getString(R.string.widget_default_pinyin))
            )
        }

        fun playWordInBackground(context: Context, word: String) {
            val speechRequest = OneTimeWorkRequestBuilder<BackgroundSpeechService>()
                .setInputData(workDataOf(Pair("word", word)))
                .build()

            val workMgr = WorkManager.getInstance(context)
            workMgr.getWorkInfoByIdLiveData(speechRequest.id).observeForever(
                object: Observer<WorkInfo> {
                override fun onChanged(workInfo: WorkInfo) {
                    if (workInfo.state == WorkInfo.State.SUCCEEDED
                        || workInfo.state == WorkInfo.State.FAILED) {

                        if (workInfo.state == WorkInfo.State.FAILED
                            && workInfo.outputData.getString(BackgroundSpeechService.FAILURE_REASON) == BackgroundSpeechService.FAILURE_MUTED) {
                            Toast.makeText(context, "Unmute to hear the word", Toast.LENGTH_LONG).show()
                        }

                        workMgr.getWorkInfoByIdLiveData(speechRequest.id)
                            .removeObserver(this)
                    }

                }
            })

            workMgr.enqueue(speechRequest)
        }
    }
}