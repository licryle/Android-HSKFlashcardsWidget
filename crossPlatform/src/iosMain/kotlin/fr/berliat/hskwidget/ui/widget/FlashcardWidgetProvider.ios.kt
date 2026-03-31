package fr.berliat.hskwidget.ui.widget

import fr.berliat.hskwidget.domain.WidgetProvider
import kotlin.coroutines.suspendCoroutine
import kotlin.coroutines.resume

actual class FlashcardWidgetProvider actual constructor() {
    actual fun updateAllFlashCardWidgets() {}

    actual suspend fun getWidgetIds(): List<Int> = suspendCoroutine { continuation ->
        val delegate = WidgetProvider.delegate
        if (delegate == null) {
            continuation.resume(emptyList())
            return@suspendCoroutine
        }

        delegate.getWidgetIds { ids ->
            continuation.resume(ids)
        }
    }
}
