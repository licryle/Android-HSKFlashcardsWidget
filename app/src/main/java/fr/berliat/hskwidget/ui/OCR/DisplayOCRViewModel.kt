package fr.berliat.hskwidget.ui.OCR

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel

class DisplayOCRViewModel(private val savedStateHandle: SavedStateHandle) : ViewModel() {

    val clickedWords: MutableMap<String, String> = mutableMapOf()

    // Key for scroll position
    private val SCROLL_POSITION_KEY = "scroll_position"

    // Getter and Setter for scroll position
    var scrollPosition: Int
        get() = savedStateHandle.get<Int>(SCROLL_POSITION_KEY) ?: 0
        set(value) {
            savedStateHandle.set(SCROLL_POSITION_KEY, value)
        }

    var text: String? = null

    fun resetText() {
        scrollPosition = 0
        text = ""
        clickedWords.clear()
    }
}