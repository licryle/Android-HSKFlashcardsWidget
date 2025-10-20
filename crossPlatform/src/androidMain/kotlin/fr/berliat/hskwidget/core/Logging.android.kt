package fr.berliat.hskwidget.core

import android.os.Bundle
import com.google.firebase.Firebase
import com.google.firebase.analytics.analytics
import com.google.firebase.crashlytics.crashlytics
import fr.berliat.hskwidget.ui.widget.FlashcardWidgetProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

actual object ExpectedLogging {
    internal actual fun logCrashalytics(e: Throwable) {
        Firebase.crashlytics.recordException(e)
    }

    internal actual fun logAnalyticsEvent(event: Logging.ANALYTICS_EVENTS,
                                          params: Map<String, String>) {
        val bundle = Bundle()
        params.forEach {
            bundle.putString(it.key, it.value)
        }

        val widgets = FlashcardWidgetProvider.getWidgetIds()
        bundle.putString("WIDGET_TOTAL_NUMBER", widgets.size.toString())

        if (widgets.isEmpty()) {
            bundle.putString("MAX_WIDGET_ID", "0")
        } else {
            bundle.putString("MAX_WIDGET_ID", widgets.last().toString())
        }

        HSKAppServices.appScope.launch(Dispatchers.IO) {
            Firebase.analytics.logEvent(event.name, bundle)
        }
    }

    internal actual fun logAnalyticsWidgetAction(event: Logging.ANALYTICS_EVENTS, widgetId: Int) {
        val widgets = FlashcardWidgetProvider.getWidgetIds()
        val size = FlashcardWidgetProvider.WidgetSizeProvider(ExpectedUtils.context).getWidgetsSize(widgetId)

        logAnalyticsEvent(
            event,
            mapOf(
                "WIDGET_NUMBER" to widgets.indexOf(widgetId).toString(),
                "WIDGET_SIZE" to "${size.first}x${size.second}"
            )
        )
    }
}