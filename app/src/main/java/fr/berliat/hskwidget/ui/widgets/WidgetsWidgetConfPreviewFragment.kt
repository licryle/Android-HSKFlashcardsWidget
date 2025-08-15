package fr.berliat.hskwidget.ui.widgets

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import fr.berliat.hskwidget.R
import fr.berliat.hskwidget.domain.FlashcardManager
import fr.berliat.hskwidget.domain.Utils
import fr.berliat.hskwidget.ui.widget.FlashcardWidgetConfigFragment
import fr.berliat.hskwidget.ui.widget.FlashcardFragment


private const val ARG_WIDGETID = "WIDGETID"

/**
 * Handles a Widget at a time, with preview + configuration Fragment in the main app.
 * Use the [WidgetsWidgetConfPreviewFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class WidgetsWidgetConfPreviewFragment : Fragment(), FlashcardWidgetConfigFragment.WidgetPreferenceListener  {
    var widgetExpectsIntent: Boolean = false
    private var _widgetId: Int? = null
    private var _root: View ? = null
    private var _confFragment: FlashcardWidgetConfigFragment? = null
    private var _previewFragment: FlashcardFragment? = null

    // Properties only valid between onCreateView and onDestroyView.
    private val widgetId get() = _widgetId!!
    private val root get() = _root!!
    private val confFragment get() = _confFragment!!
    private val previewFragment get() = _previewFragment!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            _widgetId = it.getInt(ARG_WIDGETID)
        }

        _confFragment = FlashcardWidgetConfigFragment.newInstance(widgetId)
        confFragment.addWidgetPreferenceListener(this)
        _previewFragment = FlashcardFragment.newInstance(widgetId)

        with(childFragmentManager.beginTransaction()) {
            add(R.id.widgets_flashcard_fragment, previewFragment)
            add(R.id.widgets_widgetconf_fragment, confFragment)
            commit()
        }
    }

    override fun onDestroy() {
        confFragment.removeWidgetPreferenceListener(this)

        with(childFragmentManager.beginTransaction()) {
            if (_confFragment != null)
                remove(confFragment)

            if (_previewFragment != null)
                remove(previewFragment)
            commitAllowingStateLoss()
        }

        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()

        Utils.logAnalyticsWidgetAction(Utils.ANALYTICS_EVENTS.WIDGET_CONFIG_VIEW, widgetId)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _root = inflater.inflate(R.layout.fragment_widgets_widget, container, false)

        return root
    }

    override fun onWidgetPreferenceChange(widgetId: Int, listId: Long, included: Boolean) {
    }

    override fun onWidgetPreferenceEmpty(widgetId: Int) {
        Toast.makeText(
            activity,
            getString(R.string.flashcard_widget_configure_empty),
            Toast.LENGTH_SHORT
        ).show()
    }

    override fun onWidgetPreferenceSaved(widgetId: Int) {
        val activity = requireActivity()
        FlashcardManager.getInstance(activity, widgetId).updateWord()
        Utils.logAnalyticsWidgetAction(Utils.ANALYTICS_EVENTS.WIDGET_RECONFIGURE, widgetId)

        Toast.makeText(
            activity,
            getString(R.string.flashcard_widget_configure_saved),
            Toast.LENGTH_SHORT
        ).show()

        if (widgetExpectsIntent) {
            val resultIntent = Intent()
            resultIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            activity.setResult(Activity.RESULT_OK, activity.intent)
            widgetExpectsIntent = false
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
        fun newInstance(widgetId: Int) =
            WidgetsWidgetConfPreviewFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_WIDGETID, widgetId)
                }
            }
    }
}