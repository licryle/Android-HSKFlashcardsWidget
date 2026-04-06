package fr.berliat.hskwidget.ui.widget

import fr.berliat.hskwidget.domain.WidgetProvider
import fr.berliat.hskwidget.domain.awaitWidgetIds

actual class FlashcardWidgetProvider actual constructor() {
    actual fun updateAllFlashCardWidgets() {
        WidgetProvider.triggerReload()
    }

    actual suspend fun getWidgetIds(): List<Int> {
        return WidgetProvider.delegate?.awaitWidgetIds() ?: emptyList()
    }
}
