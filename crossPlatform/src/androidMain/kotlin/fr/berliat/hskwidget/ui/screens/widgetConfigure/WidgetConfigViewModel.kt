package fr.berliat.hskwidget.ui.screens.widgetConfigure

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import fr.berliat.hskwidget.core.AppDispatchers
import fr.berliat.hskwidget.core.HSKAppServices
import fr.berliat.hskwidget.core.Logging
import fr.berliat.hskwidget.data.dao.WidgetListDAO
import fr.berliat.hskwidget.data.dao.WordListDAO
import fr.berliat.hskwidget.data.model.WidgetListEntry
import fr.berliat.hskwidget.data.model.WordListWithCount
import fr.berliat.hskwidget.data.store.ChineseWordsDatabase
import fr.berliat.hskwidget.data.store.WidgetPreferencesStoreProvider
import fr.berliat.hskwidget.domain.getWidgetControllerInstance

import kotlinx.coroutines.Dispatchers
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

    private val _refreshInterval = MutableStateFlow(-1L)
    val refreshInterval: StateFlow<Long> = _refreshInterval.asStateFlow()

    init {
        loadSettings()
        Logging.logAnalyticsWidgetAction(
            event = Logging.ANALYTICS_EVENTS.WIDGET_CONFIG_VIEW,
            widgetId = widgetId
        )
    }

    fun loadSettings() {
        viewModelScope.launch(AppDispatchers.IO) {
            val widgetPreferences = widgetPrefProvider.invoke(widgetId)
            _refreshInterval.value = widgetPreferences.refreshInterval.value

            val widgetLists = widgetListDAO.getListsForWidget(widgetId)
            val all = wordListDAO.getAllLists()
            _allLists.value = all
            _selectedListIds.value = widgetLists.toSet()
        }
    }

    fun savePreferences(newList: Set<Long>, newRefreshInterval: Long) {
        setLists(newList)
        setRefreshInterval(newRefreshInterval)
    }

    private fun setLists(newList: Set<Long>) {
        val entriesToAdd = newList.map { listId -> WidgetListEntry(widgetId, listId) }

        viewModelScope.launch(AppDispatchers.IO) {
            // Todo, mutex? Low priority
            widgetListDAO.deleteWidget(widgetId)
            widgetListDAO.insertListsToWidget(entriesToAdd)
            loadSettings()

            val widgetPreferences = widgetPrefProvider.invoke(widgetId)
            getWidgetControllerInstance(widgetPreferences, database)
                .updateWord()

            onSuccessfulSave?.let {
                withContext(Dispatchers.Main) {
                    it.invoke()
                }

                Logging.logAnalyticsWidgetAction(Logging.ANALYTICS_EVENTS.WIDGET_RECONFIGURE, widgetId)
            }
        }
    }

    private fun setRefreshInterval(interval: Long) {
        viewModelScope.launch(AppDispatchers.IO) {
            val widgetPreferences = widgetPrefProvider.invoke(widgetId)
            widgetPreferences.refreshInterval.value = interval

            val widgetController = getWidgetControllerInstance(widgetPreferences, database)
            widgetController.scheduleWidgetUpdate()
        }
    }
}
