package fr.berliat.hskwidget.data.dao

import androidx.room.Dao
import androidx.room.Query
import fr.berliat.hskwidget.data.model.ChineseWord

@Dao
interface ChineseWordDAO {
    @Query("SELECT * FROM chineseword WHERE simplified = :simplifiedWord")
    suspend fun findWordFromSimplified(simplifiedWord: String?): ChineseWord?

    @Query("SELECT COUNT(*) FROM ChineseWord")
    suspend fun getCount(): Int
}