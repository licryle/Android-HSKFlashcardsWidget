package fr.berliat.hskwidget.domain

import fr.berliat.hskwidget.data.model.AnnotatedChineseWord
import fr.berliat.hskwidget.data.store.ChineseWordsDatabase
import fr.berliat.hskwidget.data.store.WidgetPreferencesStore

actual class WidgetController(widgetStore: WidgetPreferencesStore, database: ChineseWordsDatabase) : CommonWidgetController(
    widgetStore, database
) {
    override suspend fun updateDesktopWidget(word: AnnotatedChineseWord?) {
        WidgetProvider.triggerReload()
    }
}

actual suspend fun getWidgetControllerInstance(
    widgetStore: WidgetPreferencesStore,
    database: ChineseWordsDatabase
): WidgetController {
    return WidgetController(widgetStore, database)
}