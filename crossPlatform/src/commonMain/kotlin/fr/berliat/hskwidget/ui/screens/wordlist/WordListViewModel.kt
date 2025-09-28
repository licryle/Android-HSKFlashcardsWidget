package fr.berliat.hskwidget.ui.screens.wordlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import fr.berliat.hskwidget.KAnkiDelegator
import fr.berliat.hskwidget.Utils
import fr.berliat.hskwidget.data.model.ChineseWord
import fr.berliat.hskwidget.data.model.WordList
import fr.berliat.hskwidget.data.model.WordListWithCount
import fr.berliat.hskwidget.data.repo.WordListRepository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WordListViewModel(
    private val repo: WordListRepository,
    private val ankiCaller: KAnkiDelegator
) : ViewModel() {
    private val _allLists = MutableStateFlow<List<WordListWithCount>>(emptyList())
    val allLists: StateFlow<List<WordListWithCount>> = _allLists

    private var _userLists = MutableStateFlow<List<WordListWithCount>>(emptyList())
    val userLists: StateFlow<List<WordListWithCount>> = _userLists

    enum class Status {
        STARTING,
        READY,
        SAVING,
        SUCCESS,
        ERROR
    }
    private val _status = MutableSharedFlow<Status>()
    val status = _status.asSharedFlow()

    private val _dismiss = MutableStateFlow(false)
    val dismiss: StateFlow<Boolean> = _dismiss

    init {
        viewModelScope.launch { _status.emit(Status.STARTING) }
        loadAllLists()
    }

    fun loadUserLists() {
        viewModelScope.launch(Dispatchers.Main) {
            try {
                _userLists.value = withContext(Dispatchers.IO) { repo.getUserLists() }
            } catch (_: Exception) {
                _allLists.value = emptyList()
                _status.emit(Status.ERROR)
                _status.emit(Status.READY)
            }
        }
    }

    fun loadAllLists() {
        viewModelScope.launch(Dispatchers.Main) {
            try {
                _allLists.value = withContext(Dispatchers.IO) { repo.getAllLists() }
                _userLists.value = withContext(Dispatchers.IO) { repo.getUserLists() }
            } catch (_: Exception) {
                _allLists.value = emptyList()
                _userLists.value = emptyList()
                _status.emit(Status.ERROR)
            } finally {
                _status.emit(Status.READY)
            }
        }
    }

    suspend fun getWordListsForWord(word: ChineseWord) : Set<Long> {
        return try {
            repo.getWordListsForWord(word.simplified).map { it -> it.id }.toSet()
        } catch (_: Exception) {
            emptySet()
        }
    }

    fun createList(name: String) {
        launchSafe {
            repo.createList(name)
            loadAllLists() // refresh lists after creating
        }

        Utils.logAnalyticsEvent(Utils.ANALYTICS_EVENTS.LIST_CREATE)
    }

    fun deleteList(list: WordList) {
        launchSafe {
            ankiCaller(repo.deleteList(list))
            loadAllLists() // refresh lists
            Utils.logAnalyticsEvent(Utils.ANALYTICS_EVENTS.LIST_DELETE)
        }
    }

    fun renameList(wordList: WordList, newName: String) {
        launchSafe {
            repo.renameList(wordList.id, newName)
            loadAllLists() // refresh lists
            Utils.logAnalyticsEvent(Utils.ANALYTICS_EVENTS.LIST_RENAME)
        }
    }

    fun saveAssociations(word: ChineseWord, selectedWordListIds : Set<Long>) {
        viewModelScope.launch(Dispatchers.Main) {
            _status.emit(Status.SAVING)
            try {
                withContext(Dispatchers.IO) {
                    ankiCaller(
                        repo.updateWordListAssociations(word.simplified, selectedWordListIds)
                    )
                }

                Utils.logAnalyticsEvent(Utils.ANALYTICS_EVENTS.LIST_MODIFY_WORD)

                _dismiss.value = true
                _status.emit(Status.SUCCESS)
            } catch (_: Exception) {
                _status.emit(Status.ERROR)
            } finally {
                _status.emit(Status.READY)
            }
        }
    }

    private fun launchSafe(block: suspend () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                block()
            } catch (_: Exception) {
                _status.emit(Status.ERROR)
            } finally {
                _status.emit(Status.READY)
            }
        }
    }
}
