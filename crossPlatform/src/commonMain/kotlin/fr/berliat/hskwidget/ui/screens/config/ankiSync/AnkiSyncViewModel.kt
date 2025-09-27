package fr.berliat.hskwidget.ui.screens.config.ankiSync

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.berliat.hskwidget.core.HSKAppServices
import fr.berliat.hskwidget.data.store.AppPreferencesStore
import kotlinx.coroutines.launch

class AnkiSyncViewModel(
    val appConfig: AppPreferencesStore = HSKAppServices.appPreferences): ViewModel() {

    val ankiActive = appConfig.ankiSaveNotes.asStateFlow()

    fun toggleAnkiActive(enabled: Boolean) {
        appConfig.ankiSaveNotes.value = enabled

        if (enabled) {
            importsAllNotesToAnkiDroid()
        }
    }

    private fun importsAllNotesToAnkiDroid() {
        viewModelScope.launch {
            //ankiDelegate.invoke(HSKAppServices.wordListRepo.syncListsToAnki())
        }
    }
}