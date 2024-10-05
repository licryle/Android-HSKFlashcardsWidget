package fr.berliat.hskwidget.domain

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration.ORIENTATION_PORTRAIT
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.lifecycle.Observer
import androidx.navigation.NavController
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import fr.berliat.hskwidget.R
import fr.berliat.hskwidget.data.dao.AnnotatedChineseWord
import fr.berliat.hskwidget.data.model.ChineseWord
import fr.berliat.hskwidget.databinding.FragmentDictionarySearchItemBinding
import fr.berliat.hskwidget.ui.widget.FlashcardWidgetProvider
import java.util.Locale
import java.util.concurrent.TimeUnit


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
                ChineseWord.Pinyins(context.getString(R.string.widget_default_pinyin)),
                0
            )
        }

        fun getOpenURLPendingIntent(context: Context, url: String): PendingIntent {
            return PendingIntent.getActivity(
                context,
                0,
                getOpenURLIntent(url),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        fun getOpenURLIntent(url: String): Intent {
            return Intent(
                Intent.ACTION_VIEW,
                Uri.parse(url)
            ).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        fun sendEmail(context: Context, address: String, subject: String = "", body: String = "") {
            context.startActivity(getOpenURLIntent(
                "mailto:$address?subject=" + Uri.encode(subject) + "&body=" + Uri.encode(body)))
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
                            || workInfo.state == WorkInfo.State.FAILED
                        ) {

                            var errStringId = R.string.speech_failure_toast_unknown
                            if (workInfo.state == WorkInfo.State.FAILED) {
                                var errId =
                                    workInfo.outputData.getString(BackgroundSpeechService.FAILURE_REASON)
                                when (errId) {
                                    BackgroundSpeechService.FAILURE_MUTED
                                    -> errStringId = R.string.speech_failure_toast_muted

                                    BackgroundSpeechService.FAILURE_INIT_FAILED
                                    -> errStringId = R.string.speech_failure_toast_init

                                    BackgroundSpeechService.FAILURE_LANG_UNSUPPORTED
                                    -> errStringId =
                                        R.string.speech_failure_toast_chinese_unsupported

                                    else -> {
                                        errStringId = R.string.speech_failure_toast_unknown
                                        errId = BackgroundSpeechService.FAILURE_UNKNOWN
                                    }
                                }

                                logAnalyticsError(context, "SPEECH", errId, "")
                                Toast.makeText(
                                    context, context.getString(errStringId),
                                    Toast.LENGTH_LONG
                                ).show()
                            }

                            workMgr.getWorkInfoByIdLiveData(speechRequest.id)
                                .removeObserver(this)
                        }
                    }
                }
            )

            workMgr.enqueue(speechRequest)
        }

        fun hideKeyboard(context: Context, view: View) {
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            view.let {
                imm.hideSoftInputFromWindow(it.windowToken, 0)
            }
        }

        /**
         * This is a workaround for a Bug in handling system wide events.
         * An empty WorkManager queue will trigger an APPWIGET_UPDATE event, which is undesired.
         * Read more at: https://www.reddit.com/r/android_devs/comments/llq2mw/question_why_should_it_be_expected_that/
         */
        fun preventUnnecessaryAppWidgetUpdates(context: Context): Boolean {
            val workInfos = WorkManager.getInstance(context).getWorkInfosByTag("always_pending_work")
            if (workInfos.get().size > 0) return false

            val alwaysPendingWork = OneTimeWorkRequestBuilder<DummyWorker>()
                .setInitialDelay(5000L, TimeUnit.DAYS)
                .addTag("always_pending_work")
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "always_pending_work",
                ExistingWorkPolicy.KEEP,
                alwaysPendingWork
            )

            return true
        }

        fun logAnalyticsEvent(
            context: Context, event: ANALYTICS_EVENTS,
            params: Map<String, String> = mapOf()
        ) {
            val bundle = Bundle()
            params.forEach {
                bundle.putString(it.key, it.value)
            }

            val appMgr = FlashcardWidgetProvider()
            val widgets = appMgr.getWidgetIds(context)
            bundle.putString("WIDGET_TOTAL_NUMBER", widgets.size.toString())

            if (widgets.isEmpty()) {
                bundle.putString("MAX_WIDGET_ID", "0")
            } else {
                bundle.putString("MAX_WIDGET_ID", widgets.last().toString())
            }

            Firebase.analytics.logEvent(event.name, bundle)
        }

        fun logAnalyticsError(context: Context, module: String, error: String, details: String) {
            logAnalyticsEvent(
                context, ANALYTICS_EVENTS.ERROR,
                mapOf(
                    "MODULE" to module,
                    "ERROR_ID" to error,
                    "DETAILS" to details
                )
            )
        }

        fun logAnalyticsWidgetAction(context: Context, event: ANALYTICS_EVENTS, widgetId: Int) {
            val widgets = FlashcardWidgetProvider().getWidgetIds(context)
            val size = WidgetSizeProvider(context).getWidgetsSize(widgetId)

            var hskLevels = ""
            FlashcardManager.getInstance(context, widgetId).getPreferenceStore().getAllowedHSK()
                .forEach() {
                    hskLevels += it.level.toString() + ","
                }
            hskLevels = hskLevels.dropLast(1)

            logAnalyticsEvent(
                context, event,
                mapOf(
                    "WIDGET_NUMBER" to widgets.indexOf(widgetId).toString(),
                    "WIDGET_SIZE" to "${size.first}x${size.second}",
                    "WIDGET_HSK" to hskLevels
                )
            )
        }

        fun logAnalyticsScreenView(context: Context, screenName: String) {
            logAnalyticsEvent(
                context, ANALYTICS_EVENTS.SCREEN_VIEW,
                mapOf("SCREEN_NAME" to screenName)
            )
        }

        fun populateDictionaryEntryView(binding: FragmentDictionarySearchItemBinding,
                                        word: AnnotatedChineseWord, navController: NavController) {
            var pinyins = word.word?.pinyins.toString() ?: ""
            if (pinyins == "")
                pinyins = word.annotation?.pinyins?.toString() ?: ""

            var definition = word.word?.definition?.get(Locale.ENGLISH) ?: ""
            if (definition == "")
                definition = word.annotation?.notes ?: ""

            with(binding.dictionaryItemChinese) {
                hanziText = word.simplified.toString()
                pinyinText = pinyins
            }
            var hskViz = View.VISIBLE
            if (word.word?.hskLevel == null || word.word.hskLevel == ChineseWord.HSK_Level.NOT_HSK)
                hskViz = View.INVISIBLE
            binding.dictionaryItemHskLevel.visibility = hskViz
            binding.dictionaryItemHskLevel.text = word.word?.hskLevel.toString()

            binding.dictionaryItemDefinition.text = definition

            with(binding.dictionaryItemFavorite) {
                if (word.hasAnnotation()) {
                    setImageResource(R.drawable.bookmark_heart_24px)
                    imageTintList = android.content.res.ColorStateList.valueOf(
                        androidx.core.content.ContextCompat.getColor(context, R.color.md_theme_dark_inversePrimary)
                    )
                } else {
                    setImageResource(R.drawable.bookmark_24px)
                    imageTintList = android.content.res.ColorStateList.valueOf(
                        androidx.core.content.ContextCompat.getColor(context, R.color.md_theme_dark_surface)
                    )
                }

                setOnClickListener {
                    val action = fr.berliat.hskwidget.ui.dictionary.DictionarySearchFragmentDirections.annotateWord(word.simplified!!, false)

                    navController.navigate(action)
                }
            }
        }

    }

    enum class ANALYTICS_EVENTS {
        SCREEN_VIEW,
        AUTO_WORD_CHANGE,
        ERROR,
        WIDGET_PLAY_WORD,
        WIDGET_MANUAL_WORD_CHANGE,
        WIDGET_RECONFIGURE,
        WIDGET_CONFIG_VIEW,
        WIGDET_RESIZE,
        WIGDET_ADD,
        WIGDET_REMOVE,
        WIDGET_OPEN_DICTIONARY
    }

    /* Thank to https://stackoverflow.com/questions/25153604/get-the-size-of-my-homescreen-widget */
    class WidgetSizeProvider(
        private val context: Context // Do not pass Application context
    ) {

        private val appWidgetManager = AppWidgetManager.getInstance(context)

        fun getWidgetsSize(widgetId: Int): Pair<Int, Int> {
            val isPortrait = context.resources.configuration.orientation == ORIENTATION_PORTRAIT
            val width = getWidgetWidth(isPortrait, widgetId)
            val height = getWidgetHeight(isPortrait, widgetId)
            val widthInPx = context.dip(width)
            val heightInPx = context.dip(height)
            return widthInPx to heightInPx
        }

        private fun getWidgetWidth(isPortrait: Boolean, widgetId: Int): Int =
            if (isPortrait) {
                getWidgetSizeInDp(widgetId, AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
            } else {
                getWidgetSizeInDp(widgetId, AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH)
            }

        private fun getWidgetHeight(isPortrait: Boolean, widgetId: Int): Int =
            if (isPortrait) {
                getWidgetSizeInDp(widgetId, AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT)
            } else {
                getWidgetSizeInDp(widgetId, AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)
            }

        private fun getWidgetSizeInDp(widgetId: Int, key: String): Int =
            appWidgetManager.getAppWidgetOptions(widgetId).getInt(key, 0)

        private fun Context.dip(value: Int): Int =
            (value * resources.displayMetrics.density).toInt()
    }

}