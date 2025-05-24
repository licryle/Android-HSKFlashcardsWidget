package fr.berliat.hskwidget.ui.wordlist

import fr.berliat.hskwidget.data.model.WordList
import fr.berliat.hskwidget.data.model.WordListWithCount
import fr.berliat.hskwidget.data.repo.WordListRepository
import kotlinx.coroutines.*

class WordListViewModel(val wordListRepo: WordListRepository) {
    private val viewModelScope = CoroutineScope(Dispatchers.Main + SupervisorJob())


    fun getUserWordLists(callback: (List<WordListWithCount>, Exception?) -> Unit) {
        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    wordListRepo.getUserLists()
                }
                callback(result, null)
            } catch (e: Exception) {
                callback(emptyList(), e)
            }
        }
    }

    fun getAllLists(callback: (List<WordListWithCount>, Exception?) -> Unit) {
        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    wordListRepo.getAllLists()
                }
                callback(result, null)
            } catch (e: Exception) {
                callback(emptyList(), e)
            }
        }
    }

    fun getWordListsForWord(wordId: String, callback: (List<WordListWithCount>, Exception?) -> Unit) {
        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    wordListRepo.getWordListsForWord(wordId)
                }
                callback(result, null) // already on Main
            } catch (e: Exception) {
                callback(emptyList(), e)
            }
        }
    }


    fun createList(name: String, callback: (Exception?) -> Unit) {
        launchSafe(callback) {
            wordListRepo.createList(name)
        }
    }

    fun deleteList(list: WordList, callback: (Exception?) -> Unit) {
        launchSafe(callback) {
            wordListRepo.delegateToAnki(wordListRepo.deleteList(list))
        }
    }

    fun renameList(id: Long, newName: String, callback: (Exception?) -> Unit) {
        launchSafe(callback) {
            wordListRepo.renameList(id, newName)
        }
    }

    fun updateWordListAssociations(simplified: String, listIds: List<Long>, callback: (Exception?) -> Unit) {
        launchSafe(callback) {
            wordListRepo.delegateToAnki(
                wordListRepo.updateWordListAssociations(simplified, listIds)
            )
        }
    }

    private fun launchSafe(callback: (Exception?) -> Unit, block: suspend () -> Unit) {
        viewModelScope.launch {
            var error: Exception? = null
            try {
                val result = withContext(Dispatchers.IO) {
                    block()
                }
            } catch (e: Exception) {
                error = e
            }
            callback(error)
        }
    }
}
