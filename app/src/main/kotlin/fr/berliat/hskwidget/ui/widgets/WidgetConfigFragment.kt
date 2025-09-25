package fr.berliat.hskwidget.ui.widgets

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import fr.berliat.hskwidget.R
import fr.berliat.hskwidget.core.HSKAppServices
import fr.berliat.hskwidget.domain.FlashcardManager
import fr.berliat.hskwidget.domain.Utils
import fr.berliat.hskwidget.ui.screens.widget.WidgetViewModel
import fr.berliat.hskwidget.ui.screens.widgetConfigure.WidgetConfigWithPreviewScreen
import fr.berliat.hskwidget.ui.widget.ARG_WIDGETID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Handles a Widget at a time, with preview + configuration Fragment in the main app.
 * Use the [WidgetsWidgetConfPreviewFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class WidgetsWidgetConfPreviewFragment(val expectsActivityResult: Boolean = false)
    : Fragment()  {
    private var widgetId: Int = 0


    override fun onResume() {
        super.onResume()

        if (widgetId == 0) throw IllegalStateException("No WidgetId set")

        Utils.logAnalyticsWidgetAction(Utils.ANALYTICS_EVENTS.WIDGET_CONFIG_VIEW, widgetId)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        widgetId = arguments?.getInt(ARG_WIDGETID) ?: 0

        return ComposeView(requireContext()).apply {
            setContent {
                WidgetConfigWithPreviewScreen(
                    widgetId = widgetId,
                    expectsActivityResult = expectsActivityResult,
                    onSuccessfulSave = { onWidgetPreferenceSaved(widgetId) })
            }
        }
    }

    private fun onWidgetPreferenceSaved(widgetId: Int) {
        val activity = requireActivity()
        FlashcardManager.getInstance(activity, widgetId).updateWord()

        lifecycleScope.launch(Dispatchers.IO) {
            WidgetViewModel.getInstance(
                HSKAppServices.widgetsPreferencesProvider.invoke(widgetId)
            ).updateWord()
        }

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

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param widgetId The id the widget to configure
         * @return A new instance of fragment WidgetsWidgetConfPreviewFragment.
         */
        @JvmStatic
        fun newInstance(widgetId: Int, expectsActivityResult: Boolean) =
            WidgetsWidgetConfPreviewFragment(expectsActivityResult).apply {
                arguments = Bundle().apply {
                    putInt(ARG_WIDGETID, widgetId)
                }
            }
    }
}