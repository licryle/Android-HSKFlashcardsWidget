package fr.berliat.hskwidget.ui.screens.widget

import kotlinx.coroutines.flow.StateFlow

actual class WidgetsListViewModel actual constructor() {
    actual val widgetIds: StateFlow<List<Int>>
        get() = TODO("Not yet implemented")

    actual fun addNewWidget() {
    }
}