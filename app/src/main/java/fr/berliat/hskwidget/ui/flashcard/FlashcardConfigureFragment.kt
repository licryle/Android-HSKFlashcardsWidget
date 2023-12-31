package fr.berliat.hskwidget.ui.flashcard

import android.os.Bundle
import android.util.Log
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import fr.berliat.hskwidget.R
import fr.berliat.hskwidget.data.store.FlashcardPreferencesStore
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async

class FlashcardConfigureFragment() : PreferenceFragmentCompat() {
    private var _widgetId: Int? = null
    private val prefListeners = mutableListOf<WidgetPreferenceListener>()

    // Properties only valid between onCreateView and onDestroyView.
    private val widgetId get() = _widgetId!!
    override fun onCreate(savedInstanceState: Bundle?) {
        arguments?.let {
            _widgetId = it.getInt(ARG_WIDGETID)
        }

        super.onCreate(savedInstanceState)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        val store = FlashcardPreferencesStore(requireContext(), widgetId)
        preferenceManager.preferenceDataStore = store

        setPreferencesFromResource(R.xml.flashcard_widget_configure, rootKey)

        GlobalScope.async {
            store.getAllKeys(true).forEach() {
                val pref = preferenceManager.findPreference<Preference>(it)

                if (pref != null) {
                    Log.d("FlashcardConfigureFragment", "Attaching listener to ${pref.key}")
                    pref.onPreferenceChangeListener =
                        Preference.OnPreferenceChangeListener { preference, newValue ->
                            fireWidgetPreferenceChange(preference, newValue)

                            // Reflect the newValue to Preference?
                            true
                        }
                }
            }
        }
    }

    fun addWidgetPreferenceListener(listener: WidgetPreferenceListener) {
        prefListeners.add(listener)
    }

    fun removeWidgetPreferenceListener(listener: WidgetPreferenceListener) {
        prefListeners.remove(listener)
    }

    private fun fireWidgetPreferenceChange(preference: Preference, newValue: Any) {
        prefListeners.forEach() {
            it.onWidgetPreferenceChange(widgetId, preference, newValue)
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
            FlashcardConfigureFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_WIDGETID, widgetId)
                }
            }
    }

    interface WidgetPreferenceListener {
        fun onWidgetPreferenceChange(widgetId: Int, preference: Preference, newValue: Any)
    }
}