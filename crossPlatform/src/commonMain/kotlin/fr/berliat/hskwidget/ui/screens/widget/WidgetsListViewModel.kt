package fr.berliat.hskwidget.ui.screens.widget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import fr.berliat.hskwidget.core.Utils
import fr.berliat.hskwidget.ui.widget.FlashcardWidgetProvider

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

import kotlin.time.Duration.Companion.milliseconds

open class WidgetsListViewModel: ViewModel() {
    private val _widgetIds = MutableStateFlow<List<Int>>(emptyList())
    val widgetIds: StateFlow<List<Int>> = _widgetIds

    private val _showAddWidgetInstructions = MutableStateFlow(false)
    val showAddWidgetInstructions: StateFlow<Boolean> = _showAddWidgetInstructions.asStateFlow()

    init {
        viewModelScope.launch {
            val provider = FlashcardWidgetProvider()
            while (true) {
                val ids = provider.getWidgetIds()
                _widgetIds.value = ids
                delay(500.milliseconds)
            }
        }
    }

    fun speakWord(word: String) = Utils.playWordInBackground(word)

    fun addNewWidget() {
        if (!Utils.attemptAddDesktopWidget()) {
            _showAddWidgetInstructions.value = true
        }
    }

    fun dismissAddWidgetInstructions() {
        _showAddWidgetInstructions.value = false
    }
}
