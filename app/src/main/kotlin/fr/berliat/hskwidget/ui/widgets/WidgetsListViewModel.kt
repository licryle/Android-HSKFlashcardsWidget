package fr.berliat.hskwidget.ui.widgets

import androidx.lifecycle.ViewModel

class WidgetsListViewModel: ViewModel() {
    private var lastTabPosition = 0
    var expectsActivityResult = false

    fun onToggleTab(position: Int) {
        lastTabPosition = position
    }

    fun getLastTabPosition() = lastTabPosition
}