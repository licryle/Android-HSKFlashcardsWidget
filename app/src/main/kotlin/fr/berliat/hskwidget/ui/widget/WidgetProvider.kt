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
import fr.berliat.hskwidget.core.HSKAppServices
import fr.berliat.hskwidget.domain.Utils
import fr.berliat.hskwidget.core.Locale
import fr.berliat.hskwidget.data.dao.AnnotatedChineseWordDAO
import fr.berliat.hskwidget.data.store.WidgetPreferencesStore
import fr.berliat.hskwidget.data.store.WidgetPreferencesStoreProvider
import fr.berliat.hskwidget.data.type.HSK_Level
import fr.berliat.hskwidget.domain.WidgetController
import kotlinx.coroutines.CoroutineScope

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal const val ACTION_SPEAK = "fr.berliat.hskwidget.ACTION_WIDGET_SPEAK"
internal const val ACTION_DICTIONARY = "fr.berliat.hskwidget.ACTION_DICTIONARY"

/**
 * Implementation of App Widget functionality.
 * App Widget Configuration implemented in [WidgetConfigFragment]
 */
class WidgetProvider
    : AppWidgetProvider() {
    companion object {
        private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        private val _widgetIds = MutableStateFlow(intArrayOf())
        val widgetIds = _widgetIds.asStateFlow()

        private lateinit var contextProvider: () -> Context
        private lateinit var widgetPrefProvider: WidgetPreferencesStoreProvider
        private lateinit var annotatedWordDAO: AnnotatedChineseWordDAO
        private var isInitialized = false

        suspend fun getWidgetPreferences(widgetId: Int): WidgetPreferencesStore {
            return widgetPrefProvider.invoke(widgetId)
        }
        suspend fun getWidgetController(widgetId: Int): WidgetController {
            return WidgetController.getInstance(getWidgetPreferences(widgetId))
        }

        fun init(
            contextProvider: () -> Context,
            widgetPrefProvider: WidgetPreferencesStoreProvider
                = HSKAppServices.widgetsPreferencesProvider,
            annotatedWordDAO: AnnotatedChineseWordDAO
                = HSKAppServices.database.annotatedChineseWordDAO()
        ) {
            if (!isInitialized) {
                this.contextProvider = contextProvider
                this.widgetPrefProvider = widgetPrefProvider
                this.annotatedWordDAO = annotatedWordDAO
                isInitialized = true

                // launch the coroutine once
                scope.launch {
                    widgetIds.collect { ids ->
                        ids.forEach { widgetId ->
                            val store = widgetPrefProvider(widgetId)
                            scope.launch {
                                store.currentWord.asStateFlow().collect {
                                    updateFlashCardWidget(
                                        AppWidgetManager.getInstance(Companion.contextProvider.invoke()),
                                        widgetId)
                                }
                            }
                        }
                    }
                }
            }
        }

        private const val TAG = "WidgetProvider"
        const val ACTION_CONFIGURE_LATEST = "fr.berliat.hskwidget.APPWIDGET_CONFIGURE_LATEST"

        /** Thanks to https://gist.github.com/manishcm/bd05dff09b5b1640d25f **/
        internal fun getPendingSelfIntent(context: Context?, action: String?, widgetId: Int)
                : PendingIntent? {
            val intent = Intent(context, WidgetProvider::class.java)
            intent.action = action
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)

            return PendingIntent.getBroadcast(context, widgetId, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }


        private fun updateFlashCardWidget(
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val context = contextProvider.invoke()
            scope.launch(Dispatchers.IO) {
                // Switch to the IO dispatcher to perform background work
                val simplified = getWidgetPreferences(appWidgetId).currentWord.value
                val word = annotatedWordDAO.getFromSimplified(simplified)

                // Switch back to the main thread to update UI
                withContext(Dispatchers.Main) {
                    var views: RemoteViews?
                    if (word == null || ! word.hasWord()) {
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
                            setTextViewText(R.id.flashcard_definition, word.word?.definition[Locale.ENGLISH] ?: word.annotation?.notes)
                            setTextViewText(R.id.flashcard_pinyin, word.word?.pinyins.toString())

                            setTextViewText(R.id.flashcard_hsklevel, word.word?.hskLevel.toString())
                            setViewVisibility(
                                R.id.flashcard_hsklevel,
                                Utils.hideViewIf(word.word?.hskLevel == HSK_Level.NOT_HSK)
                            )
                        }
                    }

                    // Tell the AppWidgetManager to perform an update on the current widget.
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            }
        }

        fun getWidgetIds() : IntArray {
            val context = contextProvider.invoke()
            val appWidgetMgr = AppWidgetManager.getInstance(context)

            val widgetIds = appWidgetMgr.getAppWidgetIds(
                ComponentName(context, WidgetProvider::class.java))

            if (!_widgetIds.value.contentEquals(widgetIds)) {
                _widgetIds.value = widgetIds
            }

            return widgetIds
        }
    }

    fun updateAllFlashCardWidgets() {
        val context = contextProvider.invoke()
        val widgetIds = getWidgetIds()
        onUpdate(context,
            AppWidgetManager.getInstance(context),
            widgetIds)
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        Log.i(TAG, "onUpdate")
        // There may be multiple widgets active, so update all of them
        for (appWidgetId in appWidgetIds) {
            scope.launch {
                // Switch to the IO dispatcher to perform background work
                withContext(Dispatchers.IO) {
                    getWidgetController(appWidgetId).updateWord()
                }

                // Switch back to the main thread to update UI
                withContext(Dispatchers.Main) {
                    updateFlashCardWidget(appWidgetManager, appWidgetId)
                }
            }
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        Log.i(TAG, "onDeleted")

        // When the user deletes the widget, delete the preference associated with it.
        for (widgetId in appWidgetIds) {
            Utils.logAnalyticsWidgetAction(Utils.ANALYTICS_EVENTS.WIGDET_REMOVE, widgetId)

            scope.launch(Dispatchers.IO) {
                getWidgetPreferences(widgetId).clear()
            }
        }

        getWidgetIds() // Update local value and listeners
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        Log.i(TAG, "onReceive (action: ${intent?.action})")

        if (context == null) return

        val widgetId = intent?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1) ?: -1

        scope.launch(Dispatchers.IO) {
            when (intent!!.action) {
                ACTION_CONFIGURE_LATEST -> {
                    val latestWidgetId = getWidgetIds().last()

                    val confIntent = Intent(context, MainActivity::class.java)
                    confIntent.action = ACTION_APPWIDGET_CONFIGURE
                    confIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, latestWidgetId)
                    confIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK

                    context.startActivity(confIntent)
                }

                ACTION_APPWIDGET_CONFIGURE -> {
                    // TODO background activity bug
                    val confIntent = Intent(context, MainActivity::class.java)
                    confIntent.action = ACTION_APPWIDGET_CONFIGURE
                    confIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                    confIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK

                    context.startActivity(confIntent)
                }

                ACTION_SPEAK -> {
                    getWidgetController(widgetId).speakWord()
                }

                ACTION_DICTIONARY -> {
                    getWidgetController(widgetId).openDictionary()
                }

                Intent.ACTION_BOOT_COMPLETED, AppWidgetManager.ACTION_APPWIDGET_UPDATE -> {
                    if (Utils.preventUnnecessaryAppWidgetUpdates(context)) return@launch

                    var widgetIds = IntArray(1)
                    if (widgetId == -1) {
                        widgetIds = getWidgetIds()

                        Utils.logAnalyticsEvent(Utils.ANALYTICS_EVENTS.AUTO_WORD_CHANGE)
                    } else {
                        widgetIds[0] = widgetId

                        Utils.logAnalyticsWidgetAction(
                            Utils.ANALYTICS_EVENTS.WIDGET_MANUAL_WORD_CHANGE, widgetId
                        )
                    }

                    widgetIds.forEach {
                        getWidgetController(it).updateWord()
                    }
                }

                else -> {
                    super.onReceive(context, intent)
                }
            }
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
        onUpdate(context, appMgr, getWidgetIds())
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
}