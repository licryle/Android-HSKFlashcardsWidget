package fr.berliat.hskwidget.ui.screens.widget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import fr.berliat.hskwidget.Utils
import fr.berliat.hskwidget.core.HSKAppServices
import fr.berliat.hskwidget.data.model.ChineseWord
import fr.berliat.hskwidget.data.model.WordListWithCount
import fr.berliat.hskwidget.data.store.ChineseWordsDatabase
import fr.berliat.hskwidget.data.store.WidgetPreferencesStore
import fr.berliat.hskwidget.domain.SearchQuery
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class WidgetViewModel private constructor(
    val widgetStore: WidgetPreferencesStore,
    val database: ChineseWordsDatabase = HSKAppServices.database
    ) : ViewModel() {
    companion object {
        const val TAG = "WidgetViewModel"
        private val mutex = Mutex()
        private val instances = mutableMapOf<Int, WidgetViewModel>()

        suspend fun getInstance(widgetStore: WidgetPreferencesStore,
                                database: ChineseWordsDatabase = HSKAppServices.database): WidgetViewModel {
            instances[widgetStore.widgetId]?.let { return it }

            return mutex.withLock {
                instances[widgetStore.widgetId] ?:
                        WidgetViewModel(widgetStore, database).also { instance ->
                    instances[widgetStore.widgetId] = instance
                }
            }
        }
    }

    val widgetId = widgetStore.widgetId
    val simplified: StateFlow<String> = widgetStore.currentWord.asStateFlow()

    private val _word = MutableStateFlow<ChineseWord?>(null)
    val word = _word.asStateFlow()

    val widgetListDAO = HSKAppServices.database.widgetListDAO()
    val wordListDAO = HSKAppServices.database.wordListDAO()
    val wordDAO = HSKAppServices.database.chineseWordDAO()
    val annotatedWordDAO = HSKAppServices.database.annotatedChineseWordDAO()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            simplified.collect { s ->
                if (s.isEmpty()) {
                    updateWord()
                } else {
                    _word.value =
                        wordDAO.findWordFromSimplified(s)
                }
            }
        }
    }

    fun speakWord() {
        Utils.playWordInBackground(simplified.value)
    }

    fun updateWord() {
        viewModelScope.launch(Dispatchers.IO) {
            val allowedListIds = getAllowedLists().map { it.wordList.id }
            val newWord =
                annotatedWordDAO.getRandomWordFromLists(
                    allowedListIds,
                    arrayOf(simplified.value)
                )

            Logger.i(tag = TAG, messageString = "getNewWord: Got a new word, maybe: $newWord")

            // Persist it in preferences for cross-App convenience
            widgetStore.currentWord.value = newWord?.simplified ?: ""
        }
    }

    fun openDictionary() {
        val query = SearchQuery.fromString(word.value?.simplified ?: "")
        query.ignoreAnnotation = true

        Utils.openAppForSearchQuery(query)
    }

    suspend fun getAllowedLists(): List<WordListWithCount> = withContext(Dispatchers.IO) {
        val widgetListIds = widgetListDAO.getListsForWidget(widgetId)
        val lists = wordListDAO.getAllLists()

        return@withContext lists.filter { widgetListIds.contains(it.wordList.id) }
    }
}