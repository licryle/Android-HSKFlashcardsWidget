package fr.berliat.hskwidget.data.dao

import androidx.room.*
import fr.berliat.hskwidget.data.model.ChineseWord
import fr.berliat.hskwidget.data.model.WordList
import fr.berliat.hskwidget.data.model.WordListEntry
import fr.berliat.hskwidget.data.model.WordListWithWords
import kotlinx.coroutines.flow.Flow

@Dao
interface WordListDAO {
    @Query("SELECT * FROM word_lists ORDER BY lastModified DESC")
    fun getAllLists(): Flow<List<WordList>>

    @Transaction
    @Query("SELECT * FROM word_lists ORDER BY lastModified DESC")
    fun getAllListsWithWords(): Flow<List<WordListWithWords>>

    @Query("SELECT * FROM word_lists WHERE id = :listId")
    suspend fun getListById(listId: Long): WordList?

    @Transaction
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

    @Query("DELETE FROM word_list_entries WHERE listId = :listId")
    suspend fun deleteListEntries(listId: Long)

    @Query("SELECT COUNT(*) FROM word_list_entries WHERE listId = :listId")
    suspend fun getWordCount(listId: Long): Int

    @Query("SELECT * FROM word_list_entries WHERE listId = :listId")
    suspend fun getListEntries(listId: Long): List<WordListEntry>

    @Query("SELECT * FROM chineseword WHERE simplified IN (SELECT wordId FROM word_list_entries WHERE listId = :listId)")
    suspend fun getWordsInList(listId: Long): List<ChineseWord>

    @Transaction
    @Query("""
        SELECT wl.* FROM word_lists wl
        INNER JOIN word_list_entries wle ON wl.id = wle.listId
        WHERE wle.wordId = :wordId
        ORDER BY wl.creationDate DESC
    """)
    fun getWordListsForWord(wordId: String): Flow<List<WordListWithWords>>

    @Query("DELETE FROM word_list_entries WHERE wordId = :wordId")
    suspend fun removeWordFromAllLists(wordId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addWordToList(entry: WordListEntry)

    @Transaction
    @Query("UPDATE word_lists SET lastModified = CURRENT_TIMESTAMP WHERE id = :listId")
    suspend fun touchList(listId: Long): Int

    @Transaction
    @Query("UPDATE word_lists SET name = :name WHERE id = :listId")
    suspend fun renameList(listId: Long, name: String): Int

    @Query("SELECT COUNT(*) FROM word_lists WHERE name = :name AND id != :excludeId")
    suspend fun countByName(name: String, excludeId: Long = 0): Int
}