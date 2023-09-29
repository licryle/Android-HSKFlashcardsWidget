package fr.berliat.hskwidget.ui.widgets

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.preference.Preference
import fr.berliat.hskwidget.R
import fr.berliat.hskwidget.domain.FlashcardManager
import fr.berliat.hskwidget.domain.Utils
import fr.berliat.hskwidget.ui.flashcard.FlashcardConfigureFragment
import fr.berliat.hskwidget.ui.flashcard.FlashcardFragment


private const val ARG_WIDGETID = "WIDGETID"

/**
 * Handles a Widget at a time, with preview + configuration Fragment in the main app.
 * Use the [WidgetsWidgetConfPreviewFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class WidgetsWidgetConfPreviewFragment : Fragment() {
    private var _widgetId: Int? = null
    private var _root: View ? = null
    private var _confFragment: FlashcardConfigureFragment? = null
    private var _previewFragment: FlashcardFragment? = null
    private var _prefChangeCallback: WidgetPrefListener? = null

    // Properties only valid between onCreateView and onDestroyView.
    private val widgetId get() = _widgetId!!
    private val root get() = _root!!
    private val confFragment get() = _confFragment!!
    private val previewFragment get() = _previewFragment!!
    private val prefChangeCallback get() = _prefChangeCallback!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            _widgetId = it.getInt(ARG_WIDGETID)
        }

        _confFragment = FlashcardConfigureFragment.newInstance(widgetId)
        _prefChangeCallback = WidgetPrefListener(requireActivity(), requireContext())
        confFragment.addWidgetPreferenceListener(prefChangeCallback)
        _previewFragment = FlashcardFragment.newInstance(widgetId)

        with(childFragmentManager.beginTransaction()) {
            add(R.id.widgets_flashcard_fragment, previewFragment)
            add(R.id.widgets_widgetconf_fragment, confFragment)
            commit()
        }
    }

    override fun onDestroy() {
        confFragment.removeWidgetPreferenceListener(prefChangeCallback)

        with(childFragmentManager.beginTransaction()) {
            remove(confFragment)
            remove(previewFragment)
            commitAllowingStateLoss()
        }

        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()

        Utils.logAnalyticsWidgetAction(
            requireContext(),
            Utils.ANALYTICS_EVENTS.WIDGET_CONFIG_VIEW, widgetId
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _root = inflater.inflate(R.layout.fragment_widgets_widget, container, false)

        return root
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
        fun newInstance(widgetId: Int) =
            WidgetsWidgetConfPreviewFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_WIDGETID, widgetId)
                }
            }

        private class WidgetPrefListener(val activity: Activity, val context: Context)
            : FlashcardConfigureFragment.WidgetPreferenceListener {

            override fun onWidgetPreferenceChange(
                widgetId: Int,
                preference: Preference,
                newValue: Any
            ) {
                val resToastText =
                    if (newValue as Boolean) R.string.flashcard_widget_configure_toggle_on else R.string.flashcard_widget_configure_toggle_off

                Toast.makeText(
                    activity,
                    context.getString(resToastText, preference.key),
                    Toast.LENGTH_LONG
                ).show()

                FlashcardManager.getInstance(context, widgetId).updateWord()

                Utils.logAnalyticsWidgetAction(
                    activity,
                    Utils.ANALYTICS_EVENTS.WIDGET_RECONFIGURE, widgetId
                )
            }
        }
    }
}