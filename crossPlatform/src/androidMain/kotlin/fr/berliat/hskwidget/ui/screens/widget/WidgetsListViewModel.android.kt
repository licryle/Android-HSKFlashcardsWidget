package fr.berliat.hskwidget.ui.screens.widget

import android.app.AlertDialog
import android.appwidget.AppWidgetManager

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import fr.berliat.hskwidget.core.ExpectedUtils
import fr.berliat.hskwidget.ui.widget.FlashcardWidgetProvider

import fr.berliat.hskwidget.Res
import fr.berliat.hskwidget.domain.WidgetController
import fr.berliat.hskwidget.understood
import fr.berliat.hskwidget.widgets_add_widget
import fr.berliat.hskwidget.widgets_add_widget_instructions
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow

import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

import org.jetbrains.compose.resources.getString
import kotlin.time.Duration.Companion.milliseconds


actual class WidgetsListViewModel actual constructor(): ViewModel() {
    private val _widgetIds = MutableStateFlow<List<Int>>(emptyList())
    actual val widgetIds: StateFlow<List<Int>> = _widgetIds

    init {
        viewModelScope.launch {
            while (true) {
                val ids = FlashcardWidgetProvider.getWidgetIds()
                _widgetIds.value = ids.toList()
                delay(500.milliseconds)
            }
        }
    }

    actual fun addNewWidget() {
        val context = ExpectedUtils.context
        val appWidgetManager = AppWidgetManager.getInstance(context)

        if (appWidgetManager.isRequestPinAppWidgetSupported) {
            WidgetController.requestAddDesktopWidget(context, appWidgetManager)
        } else {
            viewModelScope.launch {
                AlertDialog.Builder(context)
                    .setTitle(getString(Res.string.widgets_add_widget))
                    .setMessage(getString(Res.string.widgets_add_widget_instructions))
                    .setPositiveButton(getString(Res.string.understood), null)
                    .show()
            }
        }
    }

    actual fun speakWord(word: String) {
        ExpectedUtils.playWordInBackground(word)
    }
}