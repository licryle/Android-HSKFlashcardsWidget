package fr.berliat.hskwidget.ui.flashcardwidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.RemoteViews
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import fr.berliat.hskwidget.MainActivity
import fr.berliat.hskwidget.R
import kotlinx.coroutines.Dispatchers.Main


class FlashcardWidgetProvider : AppWidgetProvider() {
    private val ACTION_SPEAK = "fr.berliat.hskwidget.ACTION_WIDGET_SPEAK"

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        // Perform this loop procedure for each widget that belongs to this
        // provider.
        appWidgetIds.forEach { appWidgetId ->
            // Create an Intent to launch ExampleActivity.
            val openAppIntent: PendingIntent = PendingIntent.getActivity(
                /* context = */ context,
                /* requestCode = */  0,
                /* intent = */ Intent(context, MainActivity::class.java),
                /* flags = */ PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val currentWord = "你好"
            var wordBundle = Bundle()
            wordBundle.putString("word", currentWord)

            // Get the layout for the widget and attach an on-click listener
            // to the button.
            val views: RemoteViews = RemoteViews(
                context.packageName,
                R.layout.flashcard_widget_layout
            ).apply {
                setOnClickPendingIntent(R.id.flashcard_chinese, openAppIntent)

                setOnClickPendingIntent(R.id.flashcard_speak,
                    getPendingSelfIntent(context, ACTION_SPEAK, appWidgetId, wordBundle))
                setOnClickPendingIntent(R.id.flashcard_reload,
                    getPendingSelfIntent(context, AppWidgetManager.ACTION_APPWIDGET_UPDATE,
                        appWidgetId, Bundle()))

                setTextViewText(R.id.flashcard_chinese, currentWord)
            }

            // Tell the AppWidgetManager to perform an update on the current
            // widget.
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        super.onReceive(context, intent)

        when (intent!!.action) {
            ACTION_SPEAK -> {
                val widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1)
                val word = intent.getStringExtra("word")
                if (word != "") {
                    val speechRequest = OneTimeWorkRequestBuilder<BackgroundSpeechService>()
                        .setInputData(workDataOf(Pair("word", word)))
                        .build()

                    WorkManager.getInstance(context!!).enqueue(speechRequest)
                }
            }

            AppWidgetManager.ACTION_APPWIDGET_UPDATE -> {onUpdate(context!!,
                AppWidgetManager.getInstance(context),
                intArrayOf(intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, 0))
            )}
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context?,
        appWidgetManager: AppWidgetManager?,
        appWidgetId: Int,
        newOptions: Bundle?
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
    }

    override fun onEnabled(context: Context) {
        // Enter relevant functionality for when the first widget is created
        super.onEnabled(context)
    }

    override fun onDisabled(context: Context) {
        // Enter relevant functionality for when the last widget is disabled
        super.onDisabled(context)
    }

    override fun onRestored(context: Context?, oldWidgetIds: IntArray?, newWidgetIds: IntArray?) {
        super.onRestored(context, oldWidgetIds, newWidgetIds)
    }

    override fun onDeleted(context: Context?, appWidgetIds: IntArray?) {
        super.onDeleted(context, appWidgetIds)
    }

    /** Thanks to https://gist.github.com/manishcm/bd05dff09b5b1640d25f **/
    protected fun getPendingSelfIntent(context: Context?, action: String?, widgetId: Int,
                                       extras: Bundle) : PendingIntent? {
        val intent = Intent(context, FlashcardWidgetProvider::class.java)
        intent.action = action
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
        intent.putExtras(extras)

        return PendingIntent.getBroadcast(context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }
}