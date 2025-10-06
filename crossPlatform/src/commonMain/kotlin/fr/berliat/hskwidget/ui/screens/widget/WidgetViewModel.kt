package fr.berliat.hskwidget.ui.screens.widget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.berliat.hskwidget.core.AppDispatchers
import fr.berliat.hskwidget.core.HSKAppServices
import fr.berliat.hskwidget.data.model.ChineseWord
import fr.berliat.hskwidget.data.store.ChineseWordsDatabase
import fr.berliat.hskwidget.data.store.WidgetPreferencesStore
import fr.berliat.hskwidget.domain.WidgetController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class WidgetViewModel(
    val widgetStore: WidgetPreferencesStore,
    val database: ChineseWordsDatabase = HSKAppServices.database
) : ViewModel() {
    private var controller : WidgetController? = null
    val wordDAO = HSKAppServices.database.chineseWordDAO()
    val simplified: StateFlow<String> = widgetStore.currentWord.asStateFlow()
    private val _word = MutableStateFlow<ChineseWord?>(null)
    val word = _word.asStateFlow()

    init {
        viewModelScope.launch(AppDispatchers.IO) {
            controller = WidgetController.getInstance(widgetStore, database)
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

    fun speakWord() = controller?.speakWord()
    fun openDictionary() = controller?.openDictionary()
    fun updateWord() {
        viewModelScope.launch(AppDispatchers.IO) {
            controller?.updateWord()
        }
    }
}