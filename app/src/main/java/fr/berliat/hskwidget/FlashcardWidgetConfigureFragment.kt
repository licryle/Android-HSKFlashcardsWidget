package fr.berliat.hskwidget

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat

class FlashcardWidgetConfigureFragment(widgetId: Int) : PreferenceFragmentCompat() {
    private val widgetId = widgetId
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceManager.preferenceDataStore = WidgetPreferencesStore(requireContext(), widgetId)

        setPreferencesFromResource(R.xml.flashcard_widget_configure, rootKey)
    }
}