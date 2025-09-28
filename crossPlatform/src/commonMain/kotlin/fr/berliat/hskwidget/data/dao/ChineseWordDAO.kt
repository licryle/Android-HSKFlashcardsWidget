package fr.berliat.hskwidget.data.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import fr.berliat.hskwidget.data.model.ChineseWord

@Dao
interface ChineseWordDAO {
    @Upsert
    suspend fun upsertAll(items: List<ChineseWord>)

    @Query("SELECT * FROM chinese_word")
    suspend fun getAll(): List<ChineseWord>

    @Query("SELECT * FROM chinese_word WHERE simplified = :simplifiedWord")
    suspend fun findWordFromSimplified(simplifiedWord: String?): ChineseWord?

    @Query("SELECT COUNT(*) FROM chinese_word")
    suspend fun getCount(): Int
}