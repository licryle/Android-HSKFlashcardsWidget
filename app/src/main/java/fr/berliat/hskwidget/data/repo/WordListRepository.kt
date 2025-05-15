package fr.berliat.hskwidget.data.repo

import android.content.Context
import android.util.Log
import fr.berliat.hskwidget.data.dao.AnnotatedChineseWord
import fr.berliat.hskwidget.data.model.WordList
import fr.berliat.hskwidget.data.model.WordListEntry
import fr.berliat.hskwidget.data.store.AnkiStore
import fr.berliat.hskwidget.data.store.ChineseWordsDatabase
import fr.berliat.hskwidget.domain.AnkiDeck
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

/**
 * WordListRepository is in charge for updating words into list, both on local and Anki storage.
 *
 * The suspend functions are a bit complex. Due to the fact that Anki API can only be called after
 * having the right permissions, and app is running. There are lots of checks to be done, and the
 * permissions one must happen in the fragment thread.
 *
 * So what happens is that all suspend modifying functions here do the local DAO work and return a
 * suspend method with all Anki work to be executed in the Fragment thread. Use an
 * AnkiDelegate for (very) easy use.
 *
 * To call any method touching Anki, those who return a suspend () -> Result<Unit>, use
 * delegateToAnki. Must be used in conjunction of a Fragment/Activity. This will automatically do
 * all checks for you and call the Anki methods.
 *
 * Here's a simplistic example:
 *     override fun onCreate(savedInstanceState: Bundle?) {
 *         super.onCreate(savedInstanceState)
 *
 *         ankiDelegate = AnkiDelegate(this)
 *         ankiDelegate.workListRepo.delegateToAnki(ankiDelegate.insertWordToList(list, word))
 *     }
 *
 * If you use a viewModel, make sure to use the same workListRepo from your AnkiDelegateHelper.
 * In other words, don't instantiate WordListRepository yourself. Take it from the Helper, always.
 *
 * Beware of execution patterns, as the callbacks can mean Anki calls executing after whatever
 * element you change/delete.
 */
class WordListRepository(private val context: Context) {
    private val database = ChineseWordsDatabase.getInstance(context)
    private val ankiStore = AnkiStore(context)
    private val wordListDAO = database.wordListDAO()
    private var _cachedSystemLists: List<WordList>? = null

    protected val _uiEvents = MutableSharedFlow<UiEvent>(
        replay = 0,
        extraBufferCapacity = 10,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val uiEvents: SharedFlow<UiEvent> = _uiEvents

    suspend fun delegateToAnki(callback: (suspend () -> Result<Unit>)?) {
        callback?.let { _uiEvents.emit(UiEvent.TriggerAnkiSync(it)) }
    }

    /****** NOT TOUCHING ANKI *******/
    suspend fun getSystemLists(): List<WordList> {
        if (_cachedSystemLists == null) {
            _cachedSystemLists = database.wordListDAO().getSystemLists()
        }
        return _cachedSystemLists!!
    }

    suspend fun isWordInList(wordList: WordList, simplified: String) : Boolean {
        val existingEntries = database.wordListDAO().getEntriesForWord(simplified)
        return (existingEntries.filter { it.listId == wordList.id }.isNotEmpty())
    }

    suspend fun isNameUnique(name: String, excludeId: Long = 0): Boolean {
        return wordListDAO.countByName(name, excludeId) == 0
    }

    suspend fun createList(name: String) {
        val list = WordList(name)

        if (!isNameUnique(name)) {
            throw Exception("A word list with this name already exists")
        }

        wordListDAO.insertList(list)
    }

    suspend fun renameList(id: Long, newName: String) {
        if (!isNameUnique(newName, id)) {
            throw Exception("A word list with this name already exists")
        }

        wordListDAO.renameList(id, newName)
        wordListDAO.touchList(id)
    }

    /****** ANKI ALTERING METHODS *******/
    suspend fun syncListsToAnki(): (suspend () -> Result<Unit>)? {
        val lists = wordListDAO.getAllLists()
        val entries = wordListDAO.getAllListEntries()

        val wordList = entries.map { it.simplified }
        val annotatedChineseWords = database.annotatedChineseWordDAO().getFromSimplified(wordList)
        val words = annotatedChineseWords.associateBy { it.simplified }

        if (lists.isEmpty() || entries.isEmpty() || words.isEmpty()) return null

        return suspend {
            val nbToImport = entries.size
            var nbImported = 0
            var nbErrors = 0

            var nbDeckCreationErrors = 0
            Log.i(TAG, "syncListsToAnki.Anki: Creating decks if needed")
            val decks = mutableMapOf<Long, AnkiDeck>()
            for (list in lists) {
                decks[list.id] = AnkiDeck.getOrCreate(context, ankiStore, list)

                if (decks[list.id]!!.ankiId == WordList.ANKI_ID_EMPTY) {
                    nbDeckCreationErrors += 1
                }
            }
            Log.i(TAG, "syncListsToAnki.Anki: Created decks if possible: $nbDeckCreationErrors errors")

            Log.i(TAG, "syncListsToAnki.Anki: Starting iterating through lists")
            for (entry in entries) {
                val deck = decks[entry.listId]
                if (deck == null) {
                    Log.e(TAG, "syncListsToAnki.Anki: found a WordListEntry not linked to any list. Skipping entry")
                    nbErrors += 1
                    continue
                }

                if (words[entry.simplified] == null) {
                    Log.e(TAG, "syncListsToAnki.Anki: found a WordListEntry not linked to an actual word. Skipping entry")
                    nbErrors += 1
                    continue
                }

                if (deck.ankiId == WordList.ANKI_ID_EMPTY) {
                    Log.e(TAG, "syncListsToAnki.Anki: deck with no ID in Anki. Skipping entry")
                    nbErrors += 1
                    continue
                }

                val id = ankiStore.importOrUpdateCard(deck, entry, words[entry.simplified]!!)

                if (id == null) {
                    nbErrors += 1
                } else {
                    nbImported += 1
                    /* @TODO Implement call back for progress*/
                }

            }

            Log.i(TAG, "importOrUpdateAllCards: import done for $nbImported ouf of $nbToImport")

            if (nbErrors > 0) {
                Result.failure(Exception("Only $nbImported ouf of $nbToImport imported to Anki."))
            } else {
                Result.success(Unit)
            }
        }
    }

    suspend fun updateWordListAssociations(simplified: String, listIds: List<Long>): (suspend () -> Result<Unit>)? {
        // First remove all existing associations for this word
        val entries = wordListDAO.getEntriesForWord(simplified)

        val toDelete = entries.filter { ! listIds.contains(it.listId)  }
        val toAdd = listIds.filter { id -> entries.none { it.listId == id }  }

        // Then add new associations
        toAdd.forEach { listId ->
            wordListDAO.addWordToList(WordListEntry(listId, simplified))
            wordListDAO.touchList(listId)
        }

        toDelete.forEach{ entry ->
            wordListDAO.deleteWordFromList(entry.listId, simplified)
        }

        return suspend { // Now do the Anki bidding by removing notes from entries
            var nbErrors = 0
            for (toDel in toDelete) {
                if (!ankiStore.deleteCard(toDel)) {
                    nbErrors += 1
                }
            }

            val annotatedWord = database.annotatedChineseWordDAO().getFromSimplified(simplified)!!
            for (toA in toAdd) {
                val deck = AnkiDeck.getOrCreate(context, ankiStore, wordListDAO.getListById(toA)!!)
                val entry = WordListEntry(toA, simplified)
                if (ankiStore.importOrUpdateCard(deck, entry, annotatedWord) == null) {
                    nbErrors += 1
                }
            }

            if (nbErrors == 0) {
                Result.success(Unit) // Indicate success if no errors
            } else {
                // If there were errors, return a failure with the count
                Result.failure(Exception("$nbErrors errors during update of ${entries.size} entries"))
            }
        }
    }

    suspend fun deleteList(list: WordList): (suspend () -> Result<Unit>)? {
        val entries = wordListDAO.getListEntries(list.id)
        wordListDAO.deleteList(list)

        if (entries.isEmpty()) return null

        wordListDAO.deleteAllFromList(list.id)

        return suspend {
            var nbErrors = 0
            for (entry in entries) {
                if (!ankiStore.deleteCard(entry)) {
                    nbErrors += 1
                }
            }

            if (nbErrors == 0) {
                Result.success(Unit) // Indicate success if no errors
            } else {
                // If there were errors, return a failure with the count
                Result.failure(Exception("$nbErrors errors during update of ${entries.size} entries"))
            }
        }
    }

    suspend fun insertWordToList(wordList: WordList, word: AnnotatedChineseWord): (suspend () -> Result<Unit>)? {
        val entries = wordListDAO.getEntriesForWord(word.simplified)
        var entry = entries.find { it.listId == wordList.id }

        if (entry == null) {
            entry = WordListEntry(wordList.id, word.simplified)
            wordListDAO.insertWordToList(entry)
        }

        if (entry.ankiNoteId != WordList.ANKI_ID_EMPTY) {
            return null
        }

        return suspend { // Will execute only if Anki integration is active, allowed and ready to fire
            val deck = AnkiDeck.getOrCreate(context, ankiStore, wordList)

            if (ankiStore.importOrUpdateCard(deck, entry, word) != null) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Coulnd't insert ${word.simplified} into Anki"))
            }
        }
    }

    suspend fun removeWordFromList(wordList: WordList, simplified: String): (suspend () -> Result<Unit>)? {
        if (!isWordInList(wordList, simplified)) return null

        val entries = wordListDAO.getEntriesForWord(simplified)
        val entry = entries.find { it.listId == wordList.id }

        // Checked on first list of function
        wordListDAO.deleteWordFromList(entry!!.listId, entry.simplified)

        return suspend { // Will execute only if Anki integration is active, allowed and ready to fire
            if (ankiStore.deleteCard(entry)) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Couldn't delete entry $entry"))
            }

            Result.failure(Exception("Word not in list"))
        }
    }

    suspend fun removeWordFromAllLists(simplified: String): (suspend () -> Result<Unit>)? {
        if (wordListDAO.getEntriesForWord(simplified).isEmpty()) return null

        // saving entries for callback before deleting in DB
        val entries = wordListDAO.getEntriesForWord(simplified)
        wordListDAO.removeWordFromAllLists(simplified)

        return suspend { // Will execute only if Anki integration is active, allowed and ready to fire
            var nbErrors = 0
            for (entry in entries) {
                if (!ankiStore.deleteCard(entry)) {
                    nbErrors += 1
                }
            }

            // After all entries have been processed, check if there were errors
            if (nbErrors == 0) {
                Result.success(Unit) // Indicate success if no errors
            } else {
                // If there were errors, return a failure with the count
                Result.failure(Exception("$nbErrors errors during update of ${entries.size} entries"))
            }
        }
    }

    suspend fun addWordToList(wordList: WordList, word: AnnotatedChineseWord): (suspend () -> Result<Unit>)? {
        var entry = wordListDAO.getEntriesForWord(word.simplified).find { it.listId == wordList.id }

        if (entry == null) {
            entry = WordListEntry(wordList.id, word.simplified)
            wordListDAO.addWordToList(entry)
        }

        // Anki now
        if (entry.ankiNoteId != WordList.ANKI_ID_EMPTY) return null

        return suspend { // Will execute only if Anki integration is active, allowed and ready to fire
            // Create deck if needed (shouldn't)
            val deck = AnkiDeck.getOrCreate(context, ankiStore, wordList)

            if (ankiStore.importOrUpdateCard(deck, entry, word) == null) {
                Result.failure(Exception("Couldn't add ${word.simplified} to anki"))
            } else {
                Result.success(Unit)
            }
        }
    }

    suspend fun addWordToSysAnnotatedList(word: AnnotatedChineseWord): (suspend () -> Result<Unit>)? {
        val wordList = getSystemLists()

        val annotList = wordList.find { it.name == WordList.SYSTEM_ANNOTATED_NAME }

        if (annotList == null) return null

        return addWordToList(annotList, word)
    }

    suspend fun updateInAllLists(simplified: String): (suspend () -> Result<Unit>)? {
        if (database.wordListDAO().getEntriesForWord(simplified).isEmpty()) return null

        return suspend { // Will execute only if Anki integration is active, allowed and ready to fire
            val entries = database.wordListDAO().getEntriesForWord(simplified)

            var nbErrors = 0
            for (entry in entries) {
                val wordList = database.wordListDAO().getListById(entry.listId)
                val word = database.annotatedChineseWordDAO().getFromSimplified(simplified)

                if (wordList == null || word == null) {
                    nbErrors += 1
                    continue
                }

                val deck = AnkiDeck.getOrCreate(context, ankiStore, wordList)
                if (ankiStore.importOrUpdateCard(deck, entry, word) == null) {
                    nbErrors += 1
                }
            }

            // After all entries have been processed, check if there were errors
            if (nbErrors == 0) {
                Result.success(Unit) // Indicate success if no errors
            } else {
                // If there were errors, return a failure with the count
                Result.failure(Exception("$nbErrors errors during update of ${entries.size} entries"))
            }
        }
    }

    sealed class UiEvent {
        data class TriggerAnkiSync(val action: suspend () -> Result<Unit>) : UiEvent()
    }

    companion object {
        const val TAG = "WordListRepository"
    }
}