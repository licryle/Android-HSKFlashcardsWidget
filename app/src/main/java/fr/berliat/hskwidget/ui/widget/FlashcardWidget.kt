package fr.berliat.hskwidget.ui.widget

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.RemoteViews
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import fr.berliat.hskwidget.MainActivity
import fr.berliat.hskwidget.R
import fr.berliat.hskwidget.data.model.ChineseWord
import fr.berliat.hskwidget.data.model.ChineseWords
import fr.berliat.hskwidget.data.store.ChineseWordsStore
import fr.berliat.hskwidget.data.store.WidgetPreferencesStore
import fr.berliat.hskwidget.domain.BackgroundSpeechService
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Implementation of App Widget functionality.
 * App Widget Configuration implemented in [FlashcardWidgetConfigureActivity]
 */
class FlashcardWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        Log.i("WidgetProvider", "onUpdate")
        // There may be multiple widgets active, so update all of them
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        Log.i("WidgetProvider", "onDeleted")

        // When the user deletes the widget, delete the preference associated with it.
        for (appWidgetId in appWidgetIds) {
            getWidgetPreferences(context, appWidgetId).clear()
        }
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        Log.i("WidgetProvider", "onReceive (action: " + intent?.action + ")")

        val widgetId = intent?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1) ?: -1
        val appMgr = AppWidgetManager.getInstance(context!!)


        when (intent!!.action) {
            ACTION_SPEAK -> {
                val word = intent.getStringExtra("word")
                if (word != "") {
                    val speechRequest = OneTimeWorkRequestBuilder<BackgroundSpeechService>()
                        .setInputData(workDataOf(Pair("word", word)))
                        .build()

                    WorkManager.getInstance(context).enqueue(speechRequest)
                }
            }

            AppWidgetManager.ACTION_APPWIDGET_UPDATE -> {
                var widgetIds = IntArray(1)
                if (widgetId == -1) {
                    widgetIds = appMgr.getAppWidgetIds(
                        ComponentName(context, FlashcardWidget::class.java))
                } else {
                    widgetIds[0] = widgetId
                }

                onUpdate(context, appMgr, widgetIds)
            }

            else -> { super.onReceive(context, intent) }
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context?,
        appWidgetManager: AppWidgetManager?,
        appWidgetId: Int,
        newOptions: Bundle?
    ) {
        Log.i("WidgetProvider", "onAppWidgetOptionsChanged")
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
    }

    override fun onEnabled(context: Context) {
        Log.i("WidgetProvider", "onEnabled")
        // Enter relevant functionality for when the first widget is created
        super.onEnabled(context)

        // Thanks to https://stackoverflow.com/questions/70654474/starting-workmanager-task-from-appwidgetprovider-results-in-endless-onupdate-cal
        val alwaysPendingWork = OneTimeWorkRequestBuilder<DummyWorker>()
            .setInitialDelay(5000L, TimeUnit.DAYS)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "always_pending_work",
            ExistingWorkPolicy.KEEP,
            alwaysPendingWork
        )

        val appMgr = AppWidgetManager.getInstance(context)
        onUpdate(context, appMgr, appMgr.getAppWidgetIds(
            ComponentName(context, FlashcardWidget::class.java)))
    }

    override fun onDisabled(context: Context) {
        Log.i("WidgetProvider", "onDisabled")
        // Enter relevant functionality for when the last widget is disabled
        super.onDisabled(context)

        WorkManager.getInstance(context).cancelUniqueWork("always_pending_work")
    }

    override fun onRestored(context: Context?, oldWidgetIds: IntArray?, newWidgetIds: IntArray?) {
        Log.i("WidgetProvider", "onRestored")
        super.onRestored(context, oldWidgetIds, newWidgetIds)
    }

    @SuppressLint("WorkerHasAPublicModifier")
    private class DummyWorker(context: Context, workerParams: WorkerParameters)
        : Worker(context, workerParams) {
        override fun doWork(): Result {
            return Result.success()
        }
    }
}

internal val ACTION_SPEAK = "fr.berliat.hskwidget.ACTION_WIDGET_SPEAK"

internal fun getDefaultWord(context: Context) : ChineseWord {
    return ChineseWord(context.getString(R.string.widget_default_chinese),
                        "",
                        mapOf(Locale.ENGLISH to context.getString(R.string.widget_default_english)),
                        ChineseWord.HSK_Level.HSK1,
                        ChineseWord.Pinyins(context.getString(R.string.widget_default_pinyin)))
}

internal fun getWidgetPreferences(context: Context, widgetId: Int) : WidgetPreferencesStore {
    return WidgetPreferencesStore(context, widgetId)
}

internal fun updateAppWidget(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetId: Int
) {
    val hskLevels = mutableListOf<ChineseWord.HSK_Level>()
    val preferences = getWidgetPreferences(context, appWidgetId)
    ChineseWord.HSK_Level.values().forEach {
        if (preferences.showHSK(it)) {
            hskLevels.add(it)
        }
    }

    val defaultWord = getDefaultWord(context)
    var currentWord = ChineseWordsStore.getRandomWord(context, hskLevels.toTypedArray(),
                                                      ChineseWords())

    if (currentWord == null) currentWord = defaultWord

    val wordBundle = Bundle()
    wordBundle.putString("word", currentWord.simplified)
    // Create an Intent to launch ExampleActivity.
    val openAppIntent: PendingIntent = PendingIntent.getActivity(
        /* context = */ context,
        /* requestCode = */  0,
        /* intent = */ Intent(context, MainActivity::class.java),
        /* flags = */ PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val searchWordIntent: PendingIntent = PendingIntent.getActivity(
        context,
        0,
        Intent(Intent.ACTION_VIEW, Uri.parse("https://www.omgchinese.com/dictionary/chinese/"
                + currentWord.simplified)),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    // Get the layout for the widget and attach an on-click listener
    // to the button.
    val views: RemoteViews = RemoteViews(
        context.packageName,
        R.layout.flashcard_widget
    ).apply {
        setOnClickPendingIntent(R.id.flashcard_chinese, searchWordIntent)
        setOnClickPendingIntent(R.id.flashcard_english, searchWordIntent)
        setOnClickPendingIntent(R.id.flashcard_pinyin, searchWordIntent)

        setOnClickPendingIntent(
            R.id.flashcard_speak,
            getPendingSelfIntent(context, ACTION_SPEAK, appWidgetId, wordBundle)
        )
        setOnClickPendingIntent(
            R.id.flashcard_reload,
            getPendingSelfIntent(context, AppWidgetManager.ACTION_APPWIDGET_UPDATE,
                appWidgetId, Bundle())
        )

        setTextViewText(R.id.flashcard_chinese, currentWord.simplified)
        setTextViewText(R.id.flashcard_english, currentWord.definition[Locale.ENGLISH])
        setTextViewText(R.id.flashcard_pinyin, currentWord.pinyins.toString())
        setTextViewText(R.id.flashcard_hsklevel, currentWord.HSK.toString())
    }

    // Tell the AppWidgetManager to perform an update on the current
    // widget.
    appWidgetManager.updateAppWidget(appWidgetId, views)
}

/** Thanks to https://gist.github.com/manishcm/bd05dff09b5b1640d25f **/
internal fun getPendingSelfIntent(context: Context?, action: String?, widgetId: Int,
                                  extras: Bundle) : PendingIntent? {
    val intent = Intent(context, FlashcardWidget::class.java)
    intent.action = action
    intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
    intent.putExtras(extras)

    return PendingIntent.getBroadcast(context, widgetId, intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
}