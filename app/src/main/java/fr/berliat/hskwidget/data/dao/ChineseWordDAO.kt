package fr.berliat.hskwidget.data.dao

import android.util.Log
import androidx.room.Dao
import androidx.room.Query
import fr.berliat.hskwidget.data.model.ChineseWord

@Dao
interface ChineseWordDAO {
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

    @Query("SELECT COUNT(*) FROM ChineseWord")
    suspend fun getCount(): Int
}