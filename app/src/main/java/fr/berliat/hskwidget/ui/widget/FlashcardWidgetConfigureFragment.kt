package fr.berliat.hskwidget.ui.widget

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import fr.berliat.hskwidget.R
import fr.berliat.hskwidget.data.store.WidgetPreferencesStore

class FlashcardWidgetConfigureFragment(private val widgetId: Int) : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceManager.preferenceDataStore = WidgetPreferencesStore(requireContext(), widgetId)

        setPreferencesFromResource(R.xml.flashcard_widget_configure, rootKey)
    }
}