package fr.berliat.hskwidget.data.store

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import fr.berliat.hskwidget.data.model.ChineseWord

internal val Context.dataStore: DataStore<Preferences> by preferencesDataStore("WidgetPreferenceStore")

class WidgetPreferencesStore(context: Context, widgetId: Int):
    PrefixedPreferenceDataStoreBridge(context.dataStore, widgetId.toString()) {

    fun showHSK(hskLevel: ChineseWord.HSK_Level) : Boolean {
        return getBoolean("hsk" + hskLevel.level.toString(), false)
    }
}