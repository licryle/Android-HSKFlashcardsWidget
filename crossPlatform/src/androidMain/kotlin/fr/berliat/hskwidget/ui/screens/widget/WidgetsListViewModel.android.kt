package fr.berliat.hskwidget.ui.screens.widget

import android.app.AlertDialog
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import fr.berliat.hskwidget.ExpectedUtils
import fr.berliat.hskwidget.domain.WidgetProvider

import hskflashcardswidget.crossplatform.generated.resources.Res
import hskflashcardswidget.crossplatform.generated.resources.understood
import hskflashcardswidget.crossplatform.generated.resources.widgets_add_widget
import hskflashcardswidget.crossplatform.generated.resources.widgets_add_widget_instructions
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
                val ids = WidgetProvider.getWidgetIds()
                _widgetIds.value = ids.toList()
                delay(500.milliseconds)
            }
        }
    }

    actual fun addNewWidget() {
        val context = ExpectedUtils.context
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val myProvider = ComponentName(context, WidgetProvider::class.java)

        if (appWidgetManager.isRequestPinAppWidgetSupported) {
            val confIntent = Intent(context, WidgetProvider::class.java)
            confIntent.action = WidgetProvider.ACTION_CONFIGURE_LATEST

            val callbackIntent = PendingIntent.getBroadcast(
                /* context = */ context,
                /* requestCode = */ 0,
                /* intent = */ confIntent,
                /* flags = */ PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

            appWidgetManager.requestPinAppWidget(myProvider, null, callbackIntent)
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
}