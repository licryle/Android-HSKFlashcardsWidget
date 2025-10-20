package fr.berliat.hskwidget.core

import co.touchlab.kermit.Logger
import com.google.firebase.Firebase
import com.google.firebase.crashlytics.crashlytics
import kotlinx.coroutines.CoroutineExceptionHandler

object Logging {
    val GlobalCoroutineExceptionHandler: CoroutineExceptionHandler =
        CoroutineExceptionHandler { context, exception ->
            Logger.e(
                tag = "CoroutineCrash",
                messageString = "Unhandled coroutine exception on context: $context",
                throwable = exception,
            )

            Firebase.crashlytics.recordException(exception)
        }

    fun logAnalyticsScreenView(screenName: String) {
        logAnalyticsEvent(
            ANALYTICS_EVENTS.SCREEN_VIEW,
            mapOf("SCREEN_NAME" to screenName)
        )
    }

    fun logAnalyticsEvent(event: ANALYTICS_EVENTS, params: Map<String, String> = emptyMap()) =
        ExpectedLogging.logAnalyticsEvent(event, params)

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

    fun logAnalyticsWidgetAction(event: ANALYTICS_EVENTS, widgetId: Int) =
        ExpectedLogging.logAnalyticsWidgetAction(event, widgetId)

    enum class ANALYTICS_EVENTS {
        SCREEN_VIEW,
        AUTO_WORD_CHANGE,
        ERROR, // Use logAnalyticsError for details
        WIDGET_PLAY_WORD,
        WIDGET_MANUAL_WORD_CHANGE,
        WIDGET_RECONFIGURE,
        WIDGET_CONFIG_VIEW,
        WIGDET_RESIZE,
        WIGDET_ADD,
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
        OCR_CAPTURE,
        OCR_WORD_NOTFOUND,
        OCR_WORD_FOUND,
        PURCHASE_CLICK,
        PURCHASE_FAILED,
        PURCHASE_SUCCESS
    }
}

expect object ExpectedLogging {
    fun logAnalyticsEvent(event: Logging.ANALYTICS_EVENTS,
                          params: Map<String, String> = mapOf())
    fun logAnalyticsWidgetAction(event: Logging.ANALYTICS_EVENTS, widgetId: Int)
}
