package fr.berliat.hskwidget.ui.screens.widget

import kotlinx.coroutines.flow.StateFlow

import fr.berliat.hskwidget.core.Utils

actual class WidgetsListViewModel actual constructor() {
    actual val widgetIds: StateFlow<List<Int>>
        get() = TODO("Not yet implemented")

    actual fun addNewWidget() { }

    actual fun speakWord(word: String) = Utils.playWordInBackground(word)
}