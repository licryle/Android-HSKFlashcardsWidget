package fr.berliat.hskwidget.data.dao

import androidx.room.*
import fr.berliat.hskwidget.data.model.ChineseWord
import fr.berliat.hskwidget.data.model.WordList
import fr.berliat.hskwidget.data.model.WordListEntry
import fr.berliat.hskwidget.data.model.WordListWithCount
import kotlinx.coroutines.flow.Flow
import java.time.Instant

private const val wordlist_with_count =
    "SELECT wl.*, COUNT(wle.simplified) AS word_count\n" +
            "  FROM word_list AS wl\n" +
            "  LEFT OUTER JOIN word_list_entry AS wle" +
            "    ON wl.id = wle.list_id"
private const val wordlist_with_count_groupby = " GROUP BY wl.id"

@Dao
interface WordListDAO {
    @Query("SELECT * FROM word_list_entry")
    suspend fun getAllListEntries(): List<WordListEntry>

    @Query("$wordlist_with_count WHERE list_type = 'SYSTEM' $wordlist_with_count_groupby")
    suspend fun getSystemLists(): List<WordListWithCount>

    @Query("$wordlist_with_count WHERE list_type = 'USER' $wordlist_with_count_groupby")
    suspend fun getUserLists(): List<WordListWithCount>

    @Query("$wordlist_with_count $wordlist_with_count_groupby ORDER BY last_modified DESC")
    suspend fun getAllLists(): List<WordListWithCount>

    @Query("$wordlist_with_count $wordlist_with_count_groupby  ORDER BY last_modified DESC")
    fun getAllListsFlow(): Flow<List<WordListWithCount>>

    @Query("$wordlist_with_count WHERE id = :listId  $wordlist_with_count_groupby ")
    suspend fun getListById(listId: Long): WordListWithCount?

    @Query("$wordlist_with_count WHERE simplified = :simplified $wordlist_with_count_groupby ")
    fun getWordListsForWordFlow(simplified: String): Flow<List<WordListWithCount>>

    @Query("$wordlist_with_count WHERE simplified = :simplified $wordlist_with_count_groupby ")
    suspend fun getWordListsForWord(simplified: String): List<WordListWithCount>

    @Insert
    suspend fun insertList(wordList: WordList): Long

    @Insert
    suspend fun insertAllLists(lists: List<WordList>)

    @Insert
    suspend fun insertWordToList(entry: WordListEntry)

    @Insert
    suspend fun insertAllWords(entries: List<WordListEntry>)

    @Delete
    suspend fun deleteList(wordList: WordList)

    @Query("DELETE FROM word_list_entry WHERE list_id = :listId AND simplified = :simplified")
    suspend fun deleteWordFromList(listId: Long, simplified: String)

    @Query("SELECT COUNT(*) FROM word_list_entry WHERE list_id = :listId")
    suspend fun getWordCount(listId: Long): Int

    @Query("SELECT * FROM word_list_entry WHERE list_id = :listId")
    suspend fun getListEntries(listId: Long): List<WordListEntry>

    @Query("SELECT * FROM chinese_word WHERE simplified IN (SELECT simplified FROM word_list_entry WHERE list_id = :listId)")
    suspend fun getWordsInList(listId: Long): List<ChineseWord>

    @Query("SELECT * FROM chinese_word WHERE simplified IN (SELECT simplified FROM word_list_entry WHERE list_id IN (:listIds))")
    suspend fun getWordsInLists(listIds: List<Long>): List<ChineseWord>

    @Transaction
    @Query("DELETE FROM word_list_entry WHERE simplified = :simplified")
    suspend fun removeWordFromAllLists(simplified: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addWordToList(entry: WordListEntry)

    @Transaction
    @Query("UPDATE word_list SET last_modified = :lastModified WHERE id = :listId")
    suspend fun _touchList(listId: Long, lastModified: Long): Int

    suspend fun touchList(listId: Long): Int {
        return _touchList(listId, Instant.now().toEpochMilli())
    }

    @Transaction
    @Query("UPDATE word_list SET name = :name WHERE id = :listId")
    suspend fun renameList(listId: Long, name: String): Int

    @Transaction
    @Query("UPDATE word_list SET anki_deck_id = :ankiDeckId WHERE id = :listId")
    suspend fun updateAnkiDeckId(listId: Long, ankiDeckId: Long): Int

    @Transaction
    @Query("UPDATE word_list_entry SET anki_note_id = :ankiNoteId WHERE list_id = :listId AND simplified = :simplified")
    suspend fun updateAnkiNoteId(listId: Long, simplified: String, ankiNoteId: Long): Int

    @Query("SELECT COUNT(*) FROM word_list WHERE name = :name AND id != :excludeId")
    suspend fun countByName(name: String, excludeId: Long = 0): Int

    @Query("$wordlist_with_count WHERE wl.id in (SELECT list_id FROM word_list_entry WHERE simplified = :simplified) $wordlist_with_count_groupby")
    suspend fun getListContainingWord(simplified: String): List<WordListWithCount>

    @Query("SELECT * FROM word_list_entry WHERE simplified = :simplified")
    suspend fun getEntriesForWord(simplified: String): List<WordListEntry>

    @Query("DELETE FROM word_list_entry WHERE list_id = :listId")
    suspend fun deleteAllFromList(listId: Long)

    @Query("SELECT * FROM word_list_entry")
    suspend fun getAllEntries(): List<WordListEntry>

    @Query("DELETE FROM word_list_entry")
    suspend fun deleteAllEntries()

    @Query("DELETE FROM word_list")
    suspend fun deleteAllLists()
}