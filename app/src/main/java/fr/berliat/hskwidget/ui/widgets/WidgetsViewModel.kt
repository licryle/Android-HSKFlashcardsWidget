package fr.berliat.hskwidget.ui.widgets

import androidx.lifecycle.ViewModel

class WidgetsViewModel: ViewModel() {
    private var lastTabPosition = 0

    fun onToggleTab(position: Int) {
        lastTabPosition = position
    }

    fun getLastTabPosition() = lastTabPosition
}