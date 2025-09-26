package fr.berliat.hskwidget.ui.widgets

import android.app.Activity
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast

import androidx.appcompat.app.AlertDialog
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment

import fr.berliat.hskwidget.R
import fr.berliat.hskwidget.domain.Utils
import fr.berliat.hskwidget.ui.screens.widget.WidgetsListScreen
import fr.berliat.hskwidget.ui.widget.FlashcardWidgetProvider

class WidgetsListFragment : Fragment() {
    var selectedWidgetId : Int = AppWidgetManager.INVALID_APPWIDGET_ID
    var expectsActivityResult : Boolean = false

    val widgetIds : IntArray
        get() {
            val context = requireActivity().applicationContext
            return FlashcardWidgetProvider().getWidgetIds(context)
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        handleIntent(arguments)

        return ComposeView(requireContext()).apply {
            setContent {
                WidgetsListScreen(
                    widgetIds,
                    selectedWidgetId = selectedWidgetId,
                    onAddNewWidget = ::addNewWidget,
                    onWidgetPreferenceSaved = ::onWidgetPreferenceSaved,
                    expectsActivityResult = expectsActivityResult
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()

        Utils.logAnalyticsScreenView("WidgetsList")
    }

    private fun handleIntent(arguments: Bundle?) {
        selectedWidgetId = arguments?.getInt("widgetId", AppWidgetManager.INVALID_APPWIDGET_ID) ?: AppWidgetManager.INVALID_APPWIDGET_ID
        if (selectedWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            setSingleWidgetConfigMode(selectedWidgetId)
        } else {
            setSingleWidgetConfigMode(AppWidgetManager.INVALID_APPWIDGET_ID)
        }

        // ToDo Notify widget manager a new widget may be here
    }

    fun setSingleWidgetConfigMode(intentWidgetId: Int) {
        expectsActivityResult = intentWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID
        if (!expectsActivityResult) return

        Utils.logAnalyticsWidgetAction(Utils.ANALYTICS_EVENTS.WIDGET_RECONFIGURE, intentWidgetId)
        // Consume condition so we don't come back here until next intent
        arguments?.putInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
    }

    fun addNewWidget() {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val myProvider = ComponentName(requireContext(), FlashcardWidgetProvider::class.java)

        if (appWidgetManager.isRequestPinAppWidgetSupported) {
            val confIntent = Intent(context, FlashcardWidgetProvider::class.java)
            confIntent.action = FlashcardWidgetProvider.ACTION_CONFIGURE_LATEST

            val callbackIntent = PendingIntent.getBroadcast(
                /* context = */ context,
                /* requestCode = */ 0,
                /* intent = */ confIntent,
                /* flags = */ PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

            appWidgetManager.requestPinAppWidget(myProvider, null, callbackIntent)
        } else {
            AlertDialog.Builder(requireContext())
                .setTitle(R.string.widgets_add_widget)
                .setMessage(R.string.widgets_add_widget_instructions)
                .setPositiveButton(R.string.understood, null)
                .show()
        }
    }

    private fun onWidgetPreferenceSaved(widgetId: Int) {
        val activity = requireActivity()

        Utils.logAnalyticsWidgetAction(Utils.ANALYTICS_EVENTS.WIDGET_RECONFIGURE, widgetId)

        Toast.makeText(
            activity,
            getString(R.string.flashcard_widget_configure_saved),
            Toast.LENGTH_SHORT
        ).show()

        if (expectsActivityResult) {
            val resultIntent = Intent()
            resultIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            activity.setResult(Activity.RESULT_OK, activity.intent)
            activity.finish()
        }
    }
}