package fr.berliat.hskwidget.core

actual object ExpectedLogging {
    actual fun logAnalyticsEvent(event: Logging.ANALYTICS_EVENTS,
                                 params: Map<String, String>) {
    }

    actual fun logAnalyticsWidgetAction(event: Logging.ANALYTICS_EVENTS, widgetId: Int) {
    }
}