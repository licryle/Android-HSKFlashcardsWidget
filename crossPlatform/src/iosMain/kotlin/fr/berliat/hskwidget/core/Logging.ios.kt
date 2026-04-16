package fr.berliat.hskwidget.core

import cocoapods.FirebaseAnalytics.FIRAnalytics
import cocoapods.FirebaseCrashlytics.FIRCrashlytics
import fr.berliat.hskwidget.domain.WidgetProvider
import fr.berliat.hskwidget.domain.awaitWidgetSize
import fr.berliat.hskwidget.ui.widget.FlashcardWidgetProvider
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.launch
import platform.Foundation.NSError
import platform.Foundation.NSLocalizedDescriptionKey

@OptIn(ExperimentalForeignApi::class)
actual object ExpectedLogging {
    internal actual fun logCrashalytics(e: Throwable) {
        val userInfo = mutableMapOf<Any?, Any?>()
        userInfo[NSLocalizedDescriptionKey] = e.message ?: "Unknown error"
        userInfo["KotlinStackTrace"] = e.stackTraceToString()

        val error = NSError.errorWithDomain(
            domain = "KotlinException",
            code = 0,
            userInfo = userInfo
        )
        FIRCrashlytics.crashlytics().recordError(error)
    }

    internal actual fun logAnalyticsEvent(event: Logging.ANALYTICS_EVENTS,
                                          params: Map<String, String>) {
        HSKAppServices.appScope.launch(AppDispatchers.IO) {
            val widgets = FlashcardWidgetProvider().getWidgetIds()
            
            val finalParams = params.toMutableMap<Any?, Any?>()
            finalParams["WIDGET_TOTAL_NUMBER"] = widgets.size.toString()

            if (widgets.isEmpty()) {
                finalParams["MAX_WIDGET_ID"] = "0"
            } else {
                finalParams["MAX_WIDGET_ID"] = widgets.last().toString()
            }

            FIRAnalytics.logEventWithName(event.name, finalParams)
        }
    }

    internal actual fun logAnalyticsWidgetAction(event: Logging.ANALYTICS_EVENTS, widgetId: Int) {
        HSKAppServices.appScope.launch(AppDispatchers.IO) {
            val widgets = FlashcardWidgetProvider().getWidgetIds()
            val size = WidgetProvider.delegate?.awaitWidgetSize(widgetId) ?: "UNKNOWN"
            
            val params: Map<String, String> = mapOf(
                "WIDGET_NUMBER" to widgets.indexOf(widgetId).toString(),
                "WIDGET_SIZE" to size
            )

            logAnalyticsEvent(event, params)
        }
    }
}
