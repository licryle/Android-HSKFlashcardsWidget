package fr.berliat.hskwidget.ui.widgets

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.preference.Preference
import fr.berliat.hskwidget.R
import fr.berliat.hskwidget.domain.FlashcardManager
import fr.berliat.hskwidget.ui.widget.FlashcardWidget
import fr.berliat.hskwidget.ui.widget.FlashcardWidgetConfigureFragment
import java.util.Locale


private const val ARG_WIDGETID = "WIDGETID"

/**
 * Handles a Widget at a time, with preview + configuration Fragment in the main app.
 * Use the [WidgetsWidgetFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class WidgetsWidgetFragment : Fragment() {
    private var _widgetId: Int? = null
    private var _root : View ? = null
    private var _flashcardsMfr : FlashcardManager? = null
    private var _confFragment : FlashcardWidgetConfigureFragment? = null
    private var _prefChangeCallback : WidgetPrefListener? = null

    // Properties only valid between onCreateView and onDestroyView.
    private val widgetId get() = _widgetId!!
    private val root get() = _root!!
    private val flashcardsMfr get() = _flashcardsMfr!!
    private val confFragment get() = _confFragment!!
    private val prefChangeCallback get() = _prefChangeCallback!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            _widgetId = it.getInt(ARG_WIDGETID)
        }

        _flashcardsMfr = FlashcardManager.getInstance(requireContext(), widgetId)
        flashcardsMfr.registerFragment(this)
    }

    override fun onDestroy() {
        flashcardsMfr.deregisterFragment(this)

        confFragment.removeWidgetPreferenceListener(prefChangeCallback)

        super.onDestroy()
    }

    fun updateFlashcardView() {
        val currentWord = flashcardsMfr.getCurrentWord()
        Log.i("WidgetsWidgetFragment", "Updating Fragment view with $currentWord")

        with(root.findViewById<TextView>(R.id.flashcard_chinese)) {
            setOnClickListener{ flashcardsMfr.openDictionary() }
            text = currentWord.simplified
        }

        with(root.findViewById<TextView>(R.id.flashcard_definition)) {
            setOnClickListener{ flashcardsMfr.openDictionary() }
            text = currentWord.definition[Locale.ENGLISH]
        }

        with(root.findViewById<TextView>(R.id.flashcard_pinyin)) {
            setOnClickListener{ flashcardsMfr.openDictionary() }
            text = currentWord.pinyins.toString()
        }

        with(root.findViewById<TextView>(R.id.flashcard_hsklevel)) {
            setOnClickListener{ flashcardsMfr.openDictionary() }
            text = currentWord.HSK.toString()
        }

        root.findViewById<View>(R.id.flashcard_speak).setOnClickListener{
            flashcardsMfr.playWidgetWord()
        }

        root.findViewById<View>(R.id.flashcard_reload).setOnClickListener{
            flashcardsMfr.updateWord()
        }

        root.invalidate()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _root = inflater.inflate(R.layout.fragment_widgets_widget, container, false)

        _confFragment = FlashcardWidgetConfigureFragment.newInstance(widgetId)
        _prefChangeCallback = WidgetPrefListener(requireActivity(), requireContext())
        confFragment.addWidgetPreferenceListener(prefChangeCallback)

        childFragmentManager.beginTransaction()
            .add(R.id.widgets_widget_container,
                 FlashcardWidgetConfigureFragment.newInstance(widgetId))
            .commit()

        updateFlashcardView()

        return root
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param widgetId The id the widget to configure
         * @return A new instance of fragment WidgetsWidgetFragment.
         */
        @JvmStatic
        fun newInstance(widgetId: Int) =
            WidgetsWidgetFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_WIDGETID, widgetId)
                }
            }

        private class WidgetPrefListener(val activity: Activity, val context: Context)
            : FlashcardWidgetConfigureFragment.WidgetPreferenceListener {

            override fun onWidgetPreferenceChange(
                widgetId: Int,
                preference: Preference,
                newValue: Any
            ) {
                val resToastText = if (newValue as Boolean) R.string.flashcard_widget_configure_toggle_on else R.string.flashcard_widget_configure_toggle_off

                Toast.makeText(activity, context.getString(resToastText, preference.key), Toast.LENGTH_LONG).show()
                FlashcardWidget().onUpdate(context,
                                           AppWidgetManager.getInstance(context), intArrayOf(widgetId))
            }
        }
    }
}