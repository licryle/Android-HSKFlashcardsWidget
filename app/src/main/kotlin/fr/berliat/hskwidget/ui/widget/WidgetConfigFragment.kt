package fr.berliat.hskwidget.ui.widget

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import fr.berliat.hskwidget.ui.screens.widgetConfigure.WidgetConfigScreen

class WidgetConfigFragment(val expectsActivityResult: Boolean = false) : Fragment() {
    private var widgetId = 0
    private val prefListeners = mutableListOf<WidgetPreferenceListener>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        widgetId = arguments?.getInt(ARG_WIDGETID) ?: 0

        return ComposeView(requireContext()).apply {
            setContent {
                WidgetConfigScreen(widgetId,
                    expectsActivityResult,
                    { fireWidgetPreferenceSaved() })
            }
        }
    }

    fun addWidgetPreferenceListener(listener: WidgetPreferenceListener) {
        prefListeners.add(listener)
    }

    fun removeWidgetPreferenceListener(listener: WidgetPreferenceListener) {
        prefListeners.remove(listener)
    }

    private fun fireWidgetPreferenceChange(listId: Long, included: Boolean) {
        prefListeners.forEach() {
            it.onWidgetPreferenceChange(widgetId, listId, included)
        }
    }

    private fun fireWidgetPreferenceEmpty() {
        prefListeners.forEach() {
            it.onWidgetPreferenceEmpty(widgetId)
        }
    }

    private fun fireWidgetPreferenceSaved() {
        prefListeners.forEach() {
            it.onWidgetPreferenceSaved(widgetId)
        }
    }

    companion object {
        const val TAG = "FlashcardWidgetConfigFragment"

        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param widgetId The id the widget to configure
         * @return A new instance of fragment WidgetsWidgetConfPreviewFragment.
         */
        @JvmStatic
        fun newInstance(widgetId: Int, expectsActivityResult: Boolean) =
            WidgetConfigFragment(expectsActivityResult).apply {
                arguments = Bundle().apply {
                    putInt(ARG_WIDGETID, widgetId)
                }
            }
    }

    interface WidgetPreferenceListener {
        fun onWidgetPreferenceChange(widgetId: Int, listId: Long, included: Boolean)
        fun onWidgetPreferenceSaved(widgetId: Int)
        fun onWidgetPreferenceEmpty(widgetId: Int)
    }
}