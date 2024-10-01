package fr.berliat.hskwidget.data.dao

import android.util.Log
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Delete
import androidx.room.Query
import fr.berliat.hskwidget.data.model.ChineseWord

@Dao
interface ChineseWordDAO {
    @Query("SELECT * FROM chineseword")
    suspend fun getAll(): List<ChineseWord>

    @Query("SELECT * FROM chineseword WHERE searchable_text LIKE '%' || :str || '%' ORDER BY popularity DESC LIMIT :pageSize OFFSET (:page * :pageSize)")
    suspend fun findWordFromStrLike(str: String?, page: Int = 0, pageSize: Int = 30): List<ChineseWord>

    @Query("SELECT * FROM chineseword WHERE simplified = :simplifiedWord")
    suspend fun findWordFromSimplified(simplifiedWord: String?): ChineseWord?

    suspend fun getRandomHSKWord(
        levels: Set<ChineseWord.HSK_Level>,
        bannedWords: Set<ChineseWord>
    ): ChineseWord? {
        val dict = getOnlyHSKLevels(
            levels.map { it.toString() }.toTypedArray(),
            bannedWords.map { it.simplified }.toTypedArray(), 1)
        if (dict.isEmpty()) return null

        Log.i("ChineseWordDAO", "New random word: $dict[0]")
        return dict[0]
    }

    @Query("SELECT * FROM chineseword WHERE hsk_level IN (:hskLevels) AND simplified NOT IN (:bannedWords) ORDER BY RANDOM() LIMIT :limit")
    suspend fun getOnlyHSKLevels(hskLevels: Array<String>, bannedWords: Array<String>, limit: Int): Array<ChineseWord>

    @Insert
    suspend fun insertAll(vararg users: ChineseWord)

    @Delete
    suspend fun delete(user: ChineseWord)
}