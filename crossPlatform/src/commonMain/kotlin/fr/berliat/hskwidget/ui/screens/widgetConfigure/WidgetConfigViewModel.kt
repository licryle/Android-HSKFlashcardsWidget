package fr.berliat.hskwidget.ui.screens.widgetConfigure

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.berliat.hskwidget.core.HSKAppServices
import fr.berliat.hskwidget.data.dao.WidgetListDAO
import fr.berliat.hskwidget.data.dao.WordListDAO
import fr.berliat.hskwidget.data.model.WidgetListEntry
import fr.berliat.hskwidget.data.model.WordListWithCount
import fr.berliat.hskwidget.data.store.ChineseWordsDatabase
import fr.berliat.hskwidget.data.store.WidgetPreferencesStoreProvider
import fr.berliat.hskwidget.domain.WidgetController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WidgetConfigViewModel(
    private val widgetId: Int,
    private val widgetPrefProvider: WidgetPreferencesStoreProvider = HSKAppServices.widgetsPreferencesProvider,
    private val database: ChineseWordsDatabase = HSKAppServices.database,
    private val widgetListDAO: WidgetListDAO = HSKAppServices.database.widgetListDAO(),
    private val wordListDAO: WordListDAO = HSKAppServices.database.wordListDAO(),
    private val onSuccessfulSave: (() -> Unit)? = null
) : ViewModel() {
    private val _allLists = MutableStateFlow<List<WordListWithCount>>(emptyList())
    val allLists: StateFlow<List<WordListWithCount>> = _allLists.asStateFlow()

    private val _selectedListIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedListIds: StateFlow<Set<Long>> = _selectedListIds.asStateFlow()

    init {
        loadLists()
    }

    fun loadLists() {
        viewModelScope.launch(Dispatchers.IO) {
            val widgetLists = widgetListDAO.getListsForWidget(widgetId)
            val all = wordListDAO.getAllLists()
            _allLists.value = all
            _selectedListIds.value = widgetLists.toSet()
        }
    }

    fun savePreferences(newList: Set<Long>) {
        val entriesToAdd = newList.map { listId -> WidgetListEntry(widgetId, listId) }

        viewModelScope.launch(Dispatchers.IO) {
            // Todo, mutex? Low priority
            widgetListDAO.deleteWidget(widgetId)
            widgetListDAO.insertListsToWidget(entriesToAdd)
            loadLists()

            WidgetController.getInstance(widgetPrefProvider.invoke(widgetId), database)
                .updateWord()

            onSuccessfulSave?.let {
                withContext(Dispatchers.Main) {
                    it.invoke()
                }
            }
        }
    }
}
