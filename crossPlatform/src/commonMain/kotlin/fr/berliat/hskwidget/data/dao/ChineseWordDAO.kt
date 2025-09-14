package fr.berliat.hskwidget.data.dao

import androidx.room.Dao
import androidx.room.Query
import fr.berliat.hskwidget.data.model.ChineseWord

@Dao
interface ChineseWordDAO {
    @Query("SELECT * FROM chinese_word WHERE simplified = :simplifiedWord")
    suspend fun findWordFromSimplified(simplifiedWord: String?): ChineseWord?

    @Query("SELECT COUNT(*) FROM chinese_word")
    suspend fun getCount(): Int
}