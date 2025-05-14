package fr.berliat.hskwidget.data.dao

import androidx.room.*
import fr.berliat.hskwidget.data.model.ChineseWord
import fr.berliat.hskwidget.data.model.WordList
import fr.berliat.hskwidget.data.model.WordListEntry
import fr.berliat.hskwidget.data.model.WordListWithWords
import kotlinx.coroutines.flow.Flow

@Dao
interface WordListDAO {
    @Query("SELECT * FROM word_lists ORDER BY listType DESC, lastModified DESC")
    suspend fun getAllLists(): List<WordList>

    @Query("SELECT * FROM word_list_entries")
    suspend fun getAllListEntries(): List<WordListEntry>

    @Query("SELECT * FROM word_lists WHERE listType = 'SYSTEM'")
    suspend fun getSystemLists(): List<WordList>

    @Query("SELECT * FROM word_lists ORDER BY lastModified DESC")
    fun getAllListsWithWords(): Flow<List<WordListWithWords>>

    @Query("SELECT * FROM word_lists WHERE id = :listId")
    suspend fun getListById(listId: Long): WordList?

    @Query("SELECT * FROM word_lists WHERE id = :listId")
    suspend fun getListWithWordsById(listId: Long): WordListWithWords?

    @Insert
    suspend fun insertList(wordList: WordList): Long

    @Insert
    suspend fun insertWordToList(entry: WordListEntry)

    @Insert
    suspend fun insertWordsToList(entries: List<WordListEntry>)

    @Delete
    suspend fun deleteList(wordList: WordList)

    @Query("DELETE FROM word_list_entries WHERE listId = :listId AND simplified = :simplified")
    suspend fun deleteWordFromList(listId: Long, simplified: String)

    @Query("SELECT COUNT(*) FROM word_list_entries WHERE listId = :listId")
    suspend fun getWordCount(listId: Long): Int

    @Query("SELECT * FROM word_list_entries WHERE listId = :listId")
    suspend fun getListEntries(listId: Long): List<WordListEntry>

    @Query("SELECT * FROM chineseword WHERE simplified IN (SELECT simplified FROM word_list_entries WHERE listId = :listId)")
    suspend fun getWordsInList(listId: Long): List<ChineseWord>

    @Query("""
        SELECT wl.* FROM word_lists wl
        INNER JOIN word_list_entries wle ON wl.id = wle.listId
        WHERE wle.simplified = :simplified
        ORDER BY wl.creationDate DESC
    """)
    fun getWordListsForWord(simplified: String): Flow<List<WordListWithWords>>

    @Transaction
    @Query("DELETE FROM word_list_entries WHERE simplified = :simplified")
    suspend fun removeWordFromAllLists(simplified: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addWordToList(entry: WordListEntry)

    @Transaction
    @Query("UPDATE word_lists SET lastModified = CURRENT_TIMESTAMP WHERE id = :listId")
    suspend fun touchList(listId: Long): Int

    @Transaction
    @Query("UPDATE word_lists SET name = :name WHERE id = :listId")
    suspend fun renameList(listId: Long, name: String): Int

    @Transaction
    @Query("UPDATE word_lists SET ankiDeckId = :ankiDeckId WHERE id = :listId")
    suspend fun updateAnkiDeckId(listId: Long, ankiDeckId: Long): Int

    @Transaction
    @Query("UPDATE word_list_entries SET ankiNoteId = :ankiNoteId WHERE listId = :listId AND simplified = :simplified")
    suspend fun updateAnkiNoteId(listId: Long, simplified: String, ankiNoteId: Long): Int

    @Query("SELECT COUNT(*) FROM word_lists WHERE name = :name AND id != :excludeId")
    suspend fun countByName(name: String, excludeId: Long = 0): Int

    @Query("SELECT * FROM word_lists WHERE id in (SELECT listId FROM word_list_entries WHERE simplified = :simplified)")
    suspend fun getListContainingWord(simplified: String): List<WordList>

    @Query("SELECT * FROM word_list_entries WHERE simplified = :simplified")
    suspend fun getEntriesForWord(simplified: String): List<WordListEntry>

    @Query("DELETE FROM word_list_entries WHERE listId = :listId")
    suspend fun deleteAllFromList(listId: Long)
}