package fr.berliat.hskwidget.ui.widget

import android.appwidget.AppWidgetManager
import android.os.Bundle
import android.widget.Toast
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import fr.berliat.hskwidget.R
import fr.berliat.hskwidget.data.store.WidgetPreferencesStore
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async

private const val ARG_WIDGETID = "WIDGETID"

class FlashcardWidgetConfigureFragment() : PreferenceFragmentCompat() {
    private var widgetId: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        arguments?.let {
            widgetId = it.getInt(ARG_WIDGETID)
        }

        super.onCreate(savedInstanceState)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        val store = WidgetPreferencesStore(requireContext(), widgetId!!)
        preferenceManager.preferenceDataStore = store

        setPreferencesFromResource(R.xml.flashcard_widget_configure, rootKey)

        GlobalScope.async {
            store.getAllKeys(true).forEach() {
                preferenceManager.findPreference<Preference>(it)!!.onPreferenceChangeListener =
                    Preference.OnPreferenceChangeListener { preference, newValue ->

                        // @ToDo: add a listener registration and move that to activities
                        val res_toast_text = if (newValue as Boolean) R.string.flashcard_widget_configure_toggle_on else  R.string.flashcard_widget_configure_toggle_off
                        Toast.makeText(activity, getString(res_toast_text, it), Toast.LENGTH_LONG).show()
                        FlashcardWidget().onUpdate(requireContext(), AppWidgetManager.getInstance(context), intArrayOf(widgetId!!))

                        // Reflect the newValue to Preference?
                        true
                }
            }
        }
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
            FlashcardWidgetConfigureFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_WIDGETID, widgetId)
                }
            }
    }
}


