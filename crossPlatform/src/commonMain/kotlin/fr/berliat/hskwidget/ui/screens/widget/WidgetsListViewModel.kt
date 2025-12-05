package fr.berliat.hskwidget.ui.screens.widget

import kotlinx.coroutines.flow.StateFlow

expect class WidgetsListViewModel() {
    val widgetIds: StateFlow<List<Int>>

    fun addNewWidget()

    fun speakWord(word: String)
}