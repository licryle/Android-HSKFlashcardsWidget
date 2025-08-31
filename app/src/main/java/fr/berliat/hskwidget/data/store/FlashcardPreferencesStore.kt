package fr.berliat.hskwidget.data.store

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import fr.berliat.hskwidget.data.dao.WidgetListDAO
import fr.berliat.hskwidget.data.model.WidgetListEntry
import fr.berliat.hskwidget.data.model.WordListWithCount
import kotlinx.coroutines.Deferred

internal val Context.dataStore: DataStore<Preferences> by preferencesDataStore("WidgetPreferenceStore")

class FlashcardPreferencesStore(private val context: Context, private val widgetId: Int):
    PrefixedPreferenceDataStoreBridge(context.dataStore, widgetId.toString()) {
    private suspend fun WidgetListsDAO(): WidgetListDAO {
        val inst = DatabaseHelper.getInstance(context)
        val dao = inst.widgetListDAO()

        return dao
    }
    private suspend fun WordListDAO() = DatabaseHelper.getInstance(context).wordListDAO()

    var currentSimplified : String?
        get() = this.getString(PREFERENCE_CURRENT_SIMPLIFIED,null)
        set(word) { setCurrentSimplified(word, null)  }
    fun setCurrentSimplified(word: String?, callback: Callback?) : Deferred<Preferences> {
        return this.putString(PREFERENCE_CURRENT_SIMPLIFIED, word, callback)
    }

    suspend fun getAllowedLists(): List<WordListWithCount> {
        val widgetListIds = WidgetListsDAO().getListsForWidget(widgetId)
        val lists = WordListDAO().getAllLists()
        val listIds = lists.map { it.wordList.id }

        // Lists to clean -- should be nothing but just in case
        val toDelete = widgetListIds.filter { wl -> ! listIds.contains(wl) }
        WidgetListsDAO().deleteWidgetLists(toDelete.map { it -> WidgetListEntry(widgetId, it) })

        return lists.filter { widgetListIds.contains(it.wordList.id) }
    }

    companion object {
        private const val PREFERENCE_CURRENT_SIMPLIFIED = "current_simplified"
    }
}