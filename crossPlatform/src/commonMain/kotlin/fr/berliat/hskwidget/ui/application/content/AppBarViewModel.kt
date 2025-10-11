package fr.berliat.hskwidget.ui.application.content

import androidx.lifecycle.ViewModel

import fr.berliat.hskwidget.core.HSKAppServices
import fr.berliat.hskwidget.data.store.AppPreferencesStore

class AppBarViewModel(prefsStore: AppPreferencesStore = HSKAppServices.appPreferences): ViewModel() {
    // Introducing delay to avoid loops
    val searchQuery = prefsStore.searchQuery.asStateFlow()
}