package fr.berliat.hskwidget.data.store

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import fr.berliat.hskwidget.data.model.ChineseWord
import fr.berliat.hskwidget.domain.Utils

internal val Context.dataStore: DataStore<Preferences> by preferencesDataStore("WidgetPreferenceStore")

class FlashcardPreferencesStore(private val context: Context, widgetId: Int):
    PrefixedPreferenceDataStoreBridge(context.dataStore, widgetId.toString()) {

    var currentSimplified : String
        get() {
            return this.getString(
                PREFERENCE_CURRENT_SIMPLIFIED,
                Utils.getDefaultWord(context).simplified) ?: return ""

        }
        set(word: String) {
            this.putStringBlocking(PREFERENCE_CURRENT_SIMPLIFIED, word)
        }

    private fun showHSK(hskLevel: ChineseWord.HSK_Level) : Boolean {
        return getBoolean("hsk" + hskLevel.level.toString(), false)
    }

    fun getAllowedHSK(): Set<ChineseWord.HSK_Level> {
        val hskLevels = mutableSetOf<ChineseWord.HSK_Level>()
        ChineseWord.HSK_Level.values().forEach {
            if (this.showHSK(it)) {
                hskLevels.add(it)
            }
        }

        return hskLevels.toSet()
    }

    companion object {
        private const val PREFERENCE_CURRENT_SIMPLIFIED = "current_simplified"
    }
}