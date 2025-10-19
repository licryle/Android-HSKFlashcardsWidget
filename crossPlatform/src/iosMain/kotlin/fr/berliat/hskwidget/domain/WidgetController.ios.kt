package fr.berliat.hskwidget.domain

import fr.berliat.hskwidget.data.store.ChineseWordsDatabase
import fr.berliat.hskwidget.data.store.WidgetPreferencesStore

actual class WidgetController(widgetStore: WidgetPreferencesStore, database: ChineseWordsDatabase) : CommonWidgetController(
    widgetStore, database
) {
    // Todo
}

actual suspend fun getWidgetControllerInstance(
    widgetStore: WidgetPreferencesStore,
    database: ChineseWordsDatabase
): WidgetController {
    return WidgetController(widgetStore, database)
}