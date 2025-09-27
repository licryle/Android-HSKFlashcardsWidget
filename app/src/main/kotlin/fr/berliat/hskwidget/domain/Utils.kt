package fr.berliat.hskwidget.domain

import android.Manifest
import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration.ORIENTATION_PORTRAIT
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import fr.berliat.hskwidget.ui.widget.WidgetProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import fr.berliat.hskwidget.HSKHelperApp

fun <T : Parcelable> Intent.getParcelableExtraCompat(key: String, clazz: Class<T>): T? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelableExtra(key, clazz)
    } else {
        @Suppress("DEPRECATION")
        getParcelableExtra(key) as? T
    }
}

class Utils {
    class DummyWorker(context: Context, workerParams: WorkerParameters)
        : Worker(context, workerParams) {
        override fun doWork(): Result {
            return Result.success()
        }
    }

    companion object {
        fun getAppContext() : HSKHelperApp {
            return HSKHelperApp.instance
        }

        fun getAppScope(context: Context) = (context.applicationContext as HSKHelperApp).applicationScope

        fun hideKeyboard(context: Context, view: View) {
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            view.let {
                imm.hideSoftInputFromWindow(it.windowToken, 0)
            }
        }

        fun hideViewIf(isTrue: Boolean): Int {
            return if (isTrue) {
                View.GONE
            } else {
                View.VISIBLE
            }
        }

        fun requestPermissionNotification(activity: Activity) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(
                        activity.applicationContext,
                        Manifest.permission.POST_NOTIFICATIONS
                    )
                    != PackageManager.PERMISSION_GRANTED
                ) {
                    ActivityCompat.requestPermissions(
                        activity,
                        arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                        0
                    )
                }
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
            event: ANALYTICS_EVENTS,
            params: Map<String, String> = mapOf()
        ) {
            val appContext = getAppContext()
            val bundle = Bundle()
            params.forEach {
                bundle.putString(it.key, it.value)
            }

            val widgets = WidgetProvider.getWidgetIds()
            bundle.putString("WIDGET_TOTAL_NUMBER", widgets.size.toString())

            if (widgets.isEmpty()) {
                bundle.putString("MAX_WIDGET_ID", "0")
            } else {
                bundle.putString("MAX_WIDGET_ID", widgets.last().toString())
            }

            appContext.applicationScope.launch(Dispatchers.IO) {
                Firebase.analytics.logEvent(event.name, bundle)
            }
        }

        fun logAnalyticsError(module: String, error: String, details: String) {
            logAnalyticsEvent(
                ANALYTICS_EVENTS.ERROR,
                mapOf(
                    "MODULE" to module,
                    "ERROR_ID" to error,
                    "DETAILS" to details
                )
            )
        }

        fun logAnalyticsWidgetAction(event: ANALYTICS_EVENTS, widgetId: Int) {
            val appContext = getAppContext()
            val widgets = WidgetProvider.getWidgetIds()
            val size = WidgetSizeProvider(appContext).getWidgetsSize(widgetId)

            logAnalyticsEvent(
                event,
                mapOf(
                    "WIDGET_NUMBER" to widgets.indexOf(widgetId).toString(),
                    "WIDGET_SIZE" to "${size.first}x${size.second}"
                )
            )
        }

        fun logAnalyticsScreenView(screenName: String) {
            logAnalyticsEvent(
                ANALYTICS_EVENTS.SCREEN_VIEW,
                mapOf("SCREEN_NAME" to screenName)
            )
        }

        fun containsChinese(text: String): Boolean {
            val pattern = Regex("[\u4e00-\u9fff]")
            return pattern.containsMatchIn(text)
        }

        fun formatKBToMB(bytesReceived: Long, format: String = "%.2f"): String {
            return String.format(
                format,
                bytesReceived.toDouble() / 1024 / 1024)
        }
    }

    enum class ANALYTICS_EVENTS {
        SCREEN_VIEW,
        AUTO_WORD_CHANGE,
        ERROR, // Use logAnalyticsError for details
        WIDGET_PLAY_WORD,
        WIDGET_MANUAL_WORD_CHANGE,
        WIDGET_RECONFIGURE,
        WIDGET_CONFIG_VIEW,
        WIGDET_RESIZE,
        WIGDET_ADD, // Would be great to add
        WIDGET_EXPAND,
        WIDGET_COLLAPSE,
        WIGDET_REMOVE,
        WIDGET_OPEN_DICTIONARY,
        WIDGET_COPY_WORD,
        CONFIG_BACKUP_ON,
        CONFIG_BACKUP_OFF,
        CONFIG_BACKUP_RESTORE,
        CONFIG_BACKUPCLOUD_ON, // Reserved for future use
        CONFIG_BACKUPCLOUD_OFF, // Reserved for future use
        CONFIG_BACKUPCLOUD_RESTORE,
        CONFIG_BACKUPCLOUD_BACKUP,
        CONFIG_ANKI_SYNC_ON,
        CONFIG_ANKI_SYNC_OFF,
        ANNOTATION_SAVE,
        ANNOTATION_DELETE,
        LIST_CREATE,
        LIST_DELETE,
        LIST_MODIFY_WORD,
        LIST_RENAME,
        DICT_HSK3_ON,
        DICT_HSK3_OFF,
        DICT_ANNOTATION_ON,
        DICT_ANNOTATION_OFF,
        DICT_SEARCH,
        OCR_WORD_NOTFOUND,
        OCR_WORD_FOUND,
        PURCHASE_CLICK,
        PURCHASE_FAILED,
        PURCHASE_SUCCESS
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