package fr.berliat.hskwidget.ui.screens.widget

import kotlinx.coroutines.flow.StateFlow

expect class WidgetsListViewModel() {
    val widgetIds: StateFlow<List<Int>>

    fun addNewWidget()

    fun toast(s: String)

    fun speakWord(word: String)
}