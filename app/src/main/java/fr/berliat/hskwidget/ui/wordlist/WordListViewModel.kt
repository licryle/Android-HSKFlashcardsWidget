package fr.berliat.hskwidget.ui.wordlist

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import fr.berliat.hskwidget.data.model.WordList
import fr.berliat.hskwidget.data.model.WordListEntry
import fr.berliat.hskwidget.data.model.WordListWithWords
import fr.berliat.hskwidget.data.store.ChineseWordsDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WordListViewModel(application: Application) : AndroidViewModel(application) {
    private val wordListDao = ChineseWordsDatabase.getInstance(application).wordListDAO()

    val wordLists: Flow<List<WordListWithWords>> = wordListDao.getAllListsWithWords()

    fun createList(name: String, callback: (Exception?) -> Unit) {
        viewModelScope.launch {
            val list = WordList(name)

            try {
                if (!isNameUnique(name)) {
                    throw Exception("A word list with this name already exists")
                }

                wordListDao.insertList(list)
                callback(null)
            } catch (e: Exception) {
                callback(e)
            }

        }
    }

    fun deleteList(list: WordList) {
        viewModelScope.launch {
            wordListDao.deleteList(list)
        }
    }

    fun renameList(id: Long, newName: String, callback: (Exception?) -> Unit) {
        viewModelScope.launch {
            try {
                if (!isNameUnique(newName, id)) {
                    throw Exception("A word list with this name already exists")
                }

                wordListDao.renameList(id, newName)
                wordListDao.touchList(id)
                callback(null)
            } catch (e: Exception) {
                callback(e)
            }
        }
    }

    suspend fun isNameUnique(name: String, excludeId: Long = 0): Boolean {
        return wordListDao.countByName(name, excludeId) == 0
    }

    fun getWordListsForWord(wordId: String): Flow<List<WordListWithWords>> {
        return wordListDao.getWordListsForWord(wordId)
    }

    fun updateWordListAssociations(wordId: String, listIds: List<Long>, callback: (Exception?) -> Unit) {
        viewModelScope.launch {
            try {
                // First remove all existing associations for this word
                wordListDao.removeWordFromAllLists(wordId)
                // Todo: touch all lists?

                // Then add new associations
                listIds.forEach { listId ->
                    wordListDao.addWordToList(WordListEntry(listId, wordId))
                    wordListDao.touchList(listId)
                }

                withContext(Dispatchers.Main) {
                    callback(null)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    callback(null)
                }
            }
        }
    }
} 