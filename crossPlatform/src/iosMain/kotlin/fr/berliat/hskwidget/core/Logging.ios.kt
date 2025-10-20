package fr.berliat.hskwidget.core

actual object ExpectedLogging {
    internal actual fun logCrashalytics(e: Throwable) {
    }
    internal actual fun logAnalyticsEvent(event: Logging.ANALYTICS_EVENTS,
                                 params: Map<String, String>) {
    }

    internal actual fun logAnalyticsWidgetAction(event: Logging.ANALYTICS_EVENTS, widgetId: Int) {
    }
}