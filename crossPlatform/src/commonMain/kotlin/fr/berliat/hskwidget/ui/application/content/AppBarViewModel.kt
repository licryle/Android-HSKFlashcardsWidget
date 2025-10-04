package fr.berliat.hskwidget.ui.application.content

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.berliat.hskwidget.core.HSKAppServices
import fr.berliat.hskwidget.data.store.AppPreferencesStore
import fr.berliat.hskwidget.domain.SearchQuery
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

class AppBarViewModel(val prefsStore: AppPreferencesStore = HSKAppServices.appPreferences): ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    init {
        viewModelScope.launch {
            _searchQuery
                .debounce(300) // wait 300ms after typing stops
                .distinctUntilChanged() // ignore if value is the same
                .collect { query ->
                    // only called once typing has paused
                    prefsStore.searchQuery.value = SearchQuery.fromString(query)
                }
        }
    }

    fun updateSearchQuery(search: String) {
        _searchQuery.value = search
    }
}