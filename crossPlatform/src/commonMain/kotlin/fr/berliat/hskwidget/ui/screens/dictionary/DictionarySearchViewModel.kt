package fr.berliat.hskwidget.ui.screens.dictionary

import co.touchlab.kermit.Logger
import fr.berliat.hskwidget.core.AppDispatchers
import fr.berliat.hskwidget.core.Utils
import fr.berliat.hskwidget.core.HSKAppServices
import fr.berliat.hskwidget.data.dao.AnnotatedChineseWordDAO

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

import fr.berliat.hskwidget.data.model.AnnotatedChineseWord
import fr.berliat.hskwidget.data.store.AppPreferencesStore
import kotlinx.coroutines.withContext


class DictionarySearchViewModel(private val prefsStore: AppPreferencesStore = HSKAppServices.appPreferences,
                          private val annotatedChineseWordDAO: AnnotatedChineseWordDAO = HSKAppServices.database.annotatedChineseWordDAO()
) {
    val searchQuery = prefsStore.searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<AnnotatedChineseWord>>(emptyList())
    val searchResults: StateFlow<List<AnnotatedChineseWord>> = _searchResults.asStateFlow()

    private val _hasMoreResults = MutableStateFlow<Boolean>(false)
    val hasMoreResults: StateFlow<Boolean> = _hasMoreResults.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    val showHSK3: StateFlow<Boolean> = prefsStore.dictionaryShowHSK3Definition.asStateFlow()

    val hasAnnotationFilter: StateFlow<Boolean> = prefsStore.searchFilterHasAnnotation.asStateFlow()

    private var currentPage = 0
    private val itemsPerPage = 30

    fun toggleHSK3(value: Boolean) {
        prefsStore.dictionaryShowHSK3Definition.value = value
        performSearch()

        Utils.logAnalyticsEvent(if (value) Utils.ANALYTICS_EVENTS.DICT_HSK3_ON else Utils.ANALYTICS_EVENTS.DICT_HSK3_OFF)
    }

    fun toggleHasAnnotation(value: Boolean) {
        prefsStore.searchFilterHasAnnotation.value = value
        performSearch()

        Utils.logAnalyticsEvent(if (value) Utils.ANALYTICS_EVENTS.DICT_ANNOTATION_ON else Utils.ANALYTICS_EVENTS.DICT_ANNOTATION_OFF)
    }

    fun performSearch() {
        CoroutineScope(AppDispatchers.IO).launch {
            _isLoading.value = true
            currentPage = 0
            val results = fetchResultsForPage()

            withContext(Dispatchers.Main) {
                _hasMoreResults.value = results.size == itemsPerPage
                _searchResults.value = results
                _isLoading.value = false
            }
        }

        Utils.logAnalyticsEvent(Utils.ANALYTICS_EVENTS.DICT_SEARCH)
    }

    fun loadMore() {
        if (_isLoading.value) return
        _isLoading.value = true
        CoroutineScope(AppDispatchers.IO).launch {
            val newResults = fetchResultsForPage()

            withContext(Dispatchers.Main) {
                _hasMoreResults.value = newResults.size == itemsPerPage
                _searchResults.value = _searchResults.value + newResults
                _isLoading.value = false
            }
        }
    }

    private suspend fun fetchResultsForPage(): List<AnnotatedChineseWord> {
        val searchQuery = searchQuery.value
        val listName = searchQuery.inListName
        val annotatedOnly = prefsStore.searchFilterHasAnnotation.value

        Logger.e(
            tag = TAG,
            messageString = "fetchResultsForPage(${searchQuery.toString()}) at page ${currentPage} on Thread ${Thread.currentThread().name}",
        )

        val results = if (listName != null) {
            // Search within the specified word list
            annotatedChineseWordDAO.searchFromWordList(listName, annotatedOnly && !searchQuery.ignoreAnnotation, currentPage, itemsPerPage)
                .filter { it.toString().contains(searchQuery.query) }
        } else {
            annotatedChineseWordDAO.searchFromStrLike(searchQuery.query, annotatedOnly && !searchQuery.ignoreAnnotation, currentPage, itemsPerPage)
        }

        currentPage++
        return results
    }

    fun speakWord(word: String) {
        Utils.playWordInBackground(word)
    }

    fun copyWord(word: String) {
        Utils.copyToClipBoard(word)
    }

    fun listsAssociationChanged() {
        if (searchQuery.value.inListName != null) performSearch()
    }

    companion object {
        private const val TAG = "DictionarySearchViewModel"
    }
}
