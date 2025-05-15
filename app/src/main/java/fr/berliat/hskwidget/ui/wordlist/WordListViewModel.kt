package fr.berliat.hskwidget.ui.wordlist

import android.content.Context
import fr.berliat.hskwidget.data.model.WordList
import fr.berliat.hskwidget.data.model.WordListWithCount
import fr.berliat.hskwidget.data.repo.WordListRepository
import fr.berliat.hskwidget.data.store.ChineseWordsDatabase
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class WordListViewModel(context: Context, val wordListRepo: WordListRepository) {
    private val wordListDao = ChineseWordsDatabase.getInstance(context).wordListDAO()
    private val viewModelScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    val wordLists = wordListDao.getAllListsFlow()

    fun userWordLists(): Flow<List<WordListWithCount>> {
        return wordLists.map { list ->
            list.filter { it.listType == WordList.ListType.USER }
        }
    }

    fun getWordListsForWord(wordId: String): Flow<List<WordListWithCount>> {
        return wordListDao.getWordListsForWordFlow(wordId)
    }

    fun createList(name: String, callback: (Exception?) -> Unit) {
        launchSafe(callback) {
            wordListRepo.createList(name)
        }
    }

    fun deleteList(list: WordList) {
        viewModelScope.launch {
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
                block()
            } catch (e: Exception) {
                error = e
            }
            callback(error)
        }
    }
}
