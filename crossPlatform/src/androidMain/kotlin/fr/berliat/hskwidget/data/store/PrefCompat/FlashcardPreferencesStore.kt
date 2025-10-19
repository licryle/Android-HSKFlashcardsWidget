package fr.berliat.hskwidget.data.store.PrefCompat

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

internal val Context.dataStore: DataStore<Preferences> by preferencesDataStore("WidgetPreferenceStore")

class FlashcardPreferencesStore(context: Context, widgetId: Int):
    PrefixedPreferenceDataStoreBridge(context.dataStore, widgetId.toString()) {
    var currentSimplified : String? = null
        get() = this.getString(PREFERENCE_CURRENT_SIMPLIFIED,null)
        private set

    companion object {
        private const val PREFERENCE_CURRENT_SIMPLIFIED = "current_simplified"
    }
}