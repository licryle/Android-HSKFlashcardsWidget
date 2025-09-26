package fr.berliat.hskwidget.domain

import co.touchlab.kermit.Logger
import fr.berliat.hskwidget.Utils
import fr.berliat.hskwidget.core.HSKAppServices
import fr.berliat.hskwidget.data.model.WordListWithCount
import fr.berliat.hskwidget.data.store.ChineseWordsDatabase
import fr.berliat.hskwidget.data.store.WidgetPreferencesStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class WidgetController private constructor(
    val widgetStore: WidgetPreferencesStore,
    val database: ChineseWordsDatabase = HSKAppServices.database
    ) {
    companion object {
        const val TAG = "WidgetViewModel"
        private val mutex = Mutex()
        private val instances = mutableMapOf<Int, WidgetController>()

        suspend fun getInstance(widgetStore: WidgetPreferencesStore,
                                database: ChineseWordsDatabase = HSKAppServices.database): WidgetController {
            instances[widgetStore.widgetId]?.let { return it }

            return mutex.withLock {
                instances[widgetStore.widgetId] ?:
                        WidgetController(widgetStore, database).also { instance ->
                    instances[widgetStore.widgetId] = instance
                }
            }
        }
    }

    val widgetId = widgetStore.widgetId
    val simplified: StateFlow<String> = widgetStore.currentWord.asStateFlow()

    val widgetListDAO = HSKAppServices.database.widgetListDAO()
    val wordListDAO = HSKAppServices.database.wordListDAO()
    val annotatedWordDAO = HSKAppServices.database.annotatedChineseWordDAO()


    fun speakWord() {
        Utils.playWordInBackground(simplified.value)
    }

    suspend fun updateWord() = withContext(Dispatchers.IO) {
        val allowedListIds = getAllowedLists().map { it.wordList.id }
        val newWord =
            annotatedWordDAO.getRandomWordFromLists(
                allowedListIds,
                arrayOf(simplified.value)
            )

        Logger.Companion.i(tag = TAG, messageString = "getNewWord: Got a new word, maybe: $newWord")

        // Persist it in preferences for cross-App convenience
        widgetStore.currentWord.value = newWord?.simplified ?: ""
    }

    fun openDictionary() {
        val query = SearchQuery.fromString(simplified.value)
        query.ignoreAnnotation = true

        Utils.openAppForSearchQuery(query)
    }

    suspend fun getAllowedLists(): List<WordListWithCount> = withContext(Dispatchers.IO) {
        val widgetListIds = widgetListDAO.getListsForWidget(widgetId)
        val lists = wordListDAO.getAllLists()

        return@withContext lists.filter { widgetListIds.contains(it.wordList.id) }
    }
}