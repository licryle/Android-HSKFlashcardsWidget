package fr.berliat.hskwidget

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import fr.berliat.hskwidget.data.ChineseWord
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.forEach
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("WidgetPreferenceStore")

class WidgetPreferencesStore(private val context: Context, widgetId: Int):
    PrefixedPreferenceDataStoreBridge(context.dataStore, widgetId.toString()) {

    fun showHSK(hskLevel: ChineseWord.HSK_Level) : Boolean {
        return getBoolean("hsk" + hskLevel.level.toString(), false)
    }

    fun clear() {
        GlobalScope.async {
            context.dataStore.edit {
                it.asMap().forEach {
                    entry ->
                        if (entry.key.name.startsWith(getPrefix()))
                            it.remove(entry.key)
                }
            }
        }
    }
}