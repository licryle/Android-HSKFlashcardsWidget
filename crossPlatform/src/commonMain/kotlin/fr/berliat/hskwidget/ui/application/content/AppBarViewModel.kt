package fr.berliat.hskwidget.ui.application.content

import androidx.lifecycle.ViewModel

import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged

import fr.berliat.hskwidget.core.HSKAppServices
import fr.berliat.hskwidget.data.store.AppPreferencesStore

import kotlin.time.Duration.Companion.milliseconds

class AppBarViewModel(prefsStore: AppPreferencesStore = HSKAppServices.appPreferences): ViewModel() {
    // Introducing delay to avoid loops
    val searchQuery = prefsStore.searchQuery.asStateFlow()
        .debounce(500.milliseconds)        // only emit changes every 500ms
        .distinctUntilChanged() // only emit if different
}