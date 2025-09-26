package fr.berliat.hskwidget.ui.screens.dictionary

import fr.berliat.hskwidget.Utils
import fr.berliat.hskwidget.core.HSKAppServices
import fr.berliat.hskwidget.data.dao.AnnotatedChineseWordDAO

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.IO

import fr.berliat.hskwidget.data.model.AnnotatedChineseWord
import fr.berliat.hskwidget.data.store.AppPreferencesStore
import fr.berliat.hskwidget.domain.SearchQuery


class DictionaryViewModel(val appSearchQueryProvider : () -> SearchQuery,
                          prefsStore: AppPreferencesStore = HSKAppServices.appPreferences
) {
    private val _searchQuery = MutableStateFlow(appSearchQueryProvider.invoke())
    val searchQuery: StateFlow<SearchQuery> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<AnnotatedChineseWord>>(emptyList())
    val searchResults: StateFlow<List<AnnotatedChineseWord>> = _searchResults.asStateFlow()

    private val _hasMoreResults = MutableStateFlow<Boolean>(false)
    val hasMoreResults: StateFlow<Boolean> = _hasMoreResults.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    val showHSK3: StateFlow<Boolean> = prefsStore.dictionaryShowHSK3Definition.asStateFlow()

    val hasAnnotationFilter: StateFlow<Boolean> = prefsStore.searchFilterHasAnnotation.asStateFlow()

    val appPreferences = HSKAppServices.appPreferences

    private var currentPage = 0
    private val itemsPerPage = 30
    private lateinit var dao : AnnotatedChineseWordDAO

    init {
        CoroutineScope(Dispatchers.IO).launch {
            dao = Utils.getDatabaseInstance().annotatedChineseWordDAO()
        }
    }

    fun toggleHSK3(value: Boolean) {
        appPreferences.dictionaryShowHSK3Definition.value = value
        performSearch()

        Utils.logAnalyticsEvent(if (value) Utils.ANALYTICS_EVENTS.DICT_HSK3_ON else Utils.ANALYTICS_EVENTS.DICT_HSK3_OFF)
    }

    fun toggleHasAnnotation(value: Boolean) {
        appPreferences.searchFilterHasAnnotation.value = value
        performSearch()

        Utils.logAnalyticsEvent(if (value) Utils.ANALYTICS_EVENTS.DICT_ANNOTATION_ON else Utils.ANALYTICS_EVENTS.DICT_ANNOTATION_OFF)
    }

    fun performSearch() {
        _searchQuery.value = appSearchQueryProvider.invoke()

        CoroutineScope(Dispatchers.IO).launch {
            _isLoading.value = true
            currentPage = 0
            val results = fetchResultsForPage()
            _hasMoreResults.value = results.size == itemsPerPage
            _searchResults.value = results
            _isLoading.value = false
        }

        Utils.logAnalyticsEvent(Utils.ANALYTICS_EVENTS.DICT_SEARCH)
    }

    fun loadMore() {
        CoroutineScope(Dispatchers.IO).launch {
            if (_isLoading.value) return@launch
            _isLoading.value = true
            val newResults = fetchResultsForPage()
            _hasMoreResults.value = newResults.size == itemsPerPage
            _searchResults.value = _searchResults.value + newResults
            _isLoading.value = false
        }
    }

    private suspend fun fetchResultsForPage(): List<AnnotatedChineseWord> {
        val searchQuery = _searchQuery.value
        val listName = searchQuery.inListName
        val annotatedOnly = appPreferences.searchFilterHasAnnotation.value

        val results = if (listName != null) {
            // Search within the specified word list
            dao.searchFromWordList(listName, annotatedOnly && !searchQuery.ignoreAnnotation, currentPage, itemsPerPage)
                .filter { it.toString().contains(searchQuery.query) }
        } else {
            dao.searchFromStrLike(searchQuery.query, annotatedOnly && !searchQuery.ignoreAnnotation, currentPage, itemsPerPage)
        }

        currentPage++
        return results
    }

    fun listsAssociationChanged() {
        if (searchQuery.value.inListName != null) performSearch()
    }
}
