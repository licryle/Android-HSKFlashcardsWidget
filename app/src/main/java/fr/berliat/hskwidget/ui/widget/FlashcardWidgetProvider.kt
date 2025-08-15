package fr.berliat.hskwidget.ui.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetManager.ACTION_APPWIDGET_CONFIGURE
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.RemoteViews
import androidx.work.WorkManager
import fr.berliat.hskwidget.MainActivity
import fr.berliat.hskwidget.R
import fr.berliat.hskwidget.data.model.ChineseWord
import fr.berliat.hskwidget.domain.FlashcardManager
import fr.berliat.hskwidget.domain.Utils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

internal const val ACTION_SPEAK = "fr.berliat.hskwidget.ACTION_WIDGET_SPEAK"
internal const val ACTION_DICTIONARY = "fr.berliat.hskwidget.ACTION_DICTIONARY"

/**
 * Implementation of App Widget functionality.
 * App Widget Configuration implemented in [FlashcardWidgetConfigFragment]
 */
class FlashcardWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val appScope = Utils.getAppScope(context)

        Log.i(TAG, "onUpdate")
        // There may be multiple widgets active, so update all of them
        for (appWidgetId in appWidgetIds) {
            appScope.launch {
                // Switch to the IO dispatcher to perform background work
                withContext(Dispatchers.IO) {
                    FlashcardManager.getInstance(context, appWidgetId).getNewWord()
                }

                // Switch back to the main thread to update UI
                withContext(Dispatchers.Main) {
                    updateFlashCardWidget(context, appWidgetManager, appWidgetId)
                }
            }
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        Log.i(TAG, "onDeleted")

        // When the user deletes the widget, delete the preference associated with it.
        for (widgetId in appWidgetIds) {
            Utils.logAnalyticsWidgetAction(Utils.ANALYTICS_EVENTS.WIGDET_REMOVE, widgetId)

            FlashcardManager.getInstance(context, widgetId).getPreferenceStore().clear()
        }
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        Log.i(TAG, "onReceive (action: ${intent?.action})")

        if (context == null) return

        val widgetId = intent?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1) ?: -1

        when (intent!!.action) {
            ACTION_CONFIGURE_LATEST -> {
                val latestWidgetId = getWidgetIds(context).last()

                val confIntent = Intent(context, MainActivity::class.java)
                confIntent.action = ACTION_APPWIDGET_CONFIGURE
                confIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, latestWidgetId)
                confIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK

                context.startActivity(confIntent)
            }

            ACTION_APPWIDGET_CONFIGURE -> {
                val confIntent = Intent(context, MainActivity::class.java)
                confIntent.action = ACTION_APPWIDGET_CONFIGURE
                confIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                confIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK

                context.startActivity(confIntent)
            }

            ACTION_SPEAK -> {
                FlashcardManager.getInstance(context, widgetId).playWidgetWord()
            }

            ACTION_DICTIONARY -> {
                FlashcardManager.getInstance(context, widgetId).openDictionary()
            }

            AppWidgetManager.ACTION_APPWIDGET_UPDATE -> {
                if (Utils.preventUnnecessaryAppWidgetUpdates(context)) return

                var widgetIds = IntArray(1)
                if (widgetId == -1) {
                    widgetIds = getWidgetIds(context)

                    Utils.logAnalyticsEvent(Utils.ANALYTICS_EVENTS.AUTO_WORD_CHANGE)
                } else {
                    widgetIds[0] = widgetId

                    Utils.logAnalyticsWidgetAction(
                        Utils.ANALYTICS_EVENTS.WIDGET_MANUAL_WORD_CHANGE, widgetId
                    )
                }

                widgetIds.forEach {
                    FlashcardManager.getInstance(context, it).updateWord()
                }
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
        Log.i(TAG, "onAppWidgetOptionsChanged")
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        Utils.logAnalyticsWidgetAction(Utils.ANALYTICS_EVENTS.WIGDET_RESIZE, appWidgetId)
    }

    override fun onEnabled(context: Context) {
        Log.i(TAG, "onEnabled")
        // Enter relevant functionality for when the first widget is created
        super.onEnabled(context)

        Utils.preventUnnecessaryAppWidgetUpdates(context)

        val appMgr = AppWidgetManager.getInstance(context)
        onUpdate(context, appMgr, getWidgetIds(context))
    }

    override fun onDisabled(context: Context) {
        Log.i(TAG, "onDisabled")
        // Enter relevant functionality for when the last widget is disabled
        super.onDisabled(context)

        WorkManager.getInstance(context).cancelUniqueWork("always_pending_work")
    }

    override fun onRestored(context: Context?, oldWidgetIds: IntArray?, newWidgetIds: IntArray?) {
        Log.i(TAG, "onRestored")
        super.onRestored(context, oldWidgetIds, newWidgetIds)
    }

    fun updateFlashCardWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val flashcardsMfr = FlashcardManager.getInstance(context, appWidgetId)

        Utils.getAppScope(context).launch {
            // Switch to the IO dispatcher to perform background work
            val word = withContext(Dispatchers.IO) {
                flashcardsMfr.getCurrentWord()
            }

            // Switch back to the main thread to update UI
            withContext(Dispatchers.Main) {
                var views: RemoteViews? = null
                if (word == null) {
                    Log.i(TAG, "updateFlashCardWidget ID $appWidgetId , but no word available")

                    views = RemoteViews(
                        context.packageName,
                        R.layout.flashcard_widget_not_configured
                    ).apply {
                        setOnClickPendingIntent(
                            R.id.flashcard_not_configured,
                            getPendingSelfIntent(
                                context,
                                ACTION_APPWIDGET_CONFIGURE,
                                appWidgetId
                            )
                        )
                    }
                } else {
                    Log.i(TAG, "updateFlashCardWidget ID $appWidgetId with word $word")

                    val searchWordIntent =
                        getPendingSelfIntent(context, ACTION_DICTIONARY, appWidgetId)
                    // Get the layout for the widget and attach an on-click listener
                    // to the button.
                    views = RemoteViews(
                        context.packageName,
                        R.layout.flashcard_widget
                    ).apply {
                        setOnClickPendingIntent(R.id.flashcard_chinese, searchWordIntent)
                        setOnClickPendingIntent(R.id.flashcard_definition, searchWordIntent)
                        setOnClickPendingIntent(R.id.flashcard_pinyin, searchWordIntent)

                        setOnClickPendingIntent(
                            R.id.flashcard_speak,
                            getPendingSelfIntent(context, ACTION_SPEAK, appWidgetId)
                        )
                        setOnClickPendingIntent(
                            R.id.flashcard_reload,
                            getPendingSelfIntent(
                                context,
                                AppWidgetManager.ACTION_APPWIDGET_UPDATE,
                                appWidgetId
                            )
                        )

                        setTextViewText(R.id.flashcard_chinese, word.simplified)
                        setTextViewText(R.id.flashcard_definition, word.definition[Locale.ENGLISH])
                        setTextViewText(R.id.flashcard_pinyin, word.pinyins.toString())

                        setTextViewText(R.id.flashcard_hsklevel, word.hskLevel.toString())
                        setViewVisibility(
                            R.id.flashcard_hsklevel,
                            Utils.hideViewIf(word.hskLevel == ChineseWord.HSK_Level.NOT_HSK)
                        )
                    }
                }

                // Tell the AppWidgetManager to perform an update on the current widget.
                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        }
    }

    fun getWidgetIds(context: Context) : IntArray {
        val appWidgetMgr = AppWidgetManager.getInstance(context)

        return appWidgetMgr.getAppWidgetIds(
            ComponentName(context, FlashcardWidgetProvider::class.java))
    }

    companion object {
        const val TAG = "WidgetProvider"
        const val ACTION_CONFIGURE_LATEST = "fr.berliat.hskwidget.APPWIDGET_CONFIGURE_LATEST"

        /** Thanks to https://gist.github.com/manishcm/bd05dff09b5b1640d25f **/
        internal fun getPendingSelfIntent(context: Context?, action: String?, widgetId: Int)
                : PendingIntent? {
            val intent = Intent(context, FlashcardWidgetProvider::class.java)
            intent.action = action
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)

            return PendingIntent.getBroadcast(context, widgetId, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }
    }
}