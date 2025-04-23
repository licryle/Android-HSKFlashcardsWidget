package fr.berliat.hskwidget.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import fr.berliat.hskwidget.data.model.ChineseWordFrequency

@Dao
interface ChineseWordFrequencyDAO {
    companion object {
        const val CHINESE_REGEX = "^[\\u4E00-\\u9FFF]+\$"
    }

    @Query("SELECT * FROM chinesewordfrequency")
    suspend fun getAll(): List<ChineseWordFrequency>

    @Query("SELECT * FROM chinesewordfrequency WHERE simplified = :simplifiedWord")
    suspend fun getFrequency(simplifiedWord: String?): ChineseWordFrequency?

    @Query("SELECT * FROM chinesewordfrequency WHERE simplified IN (:simplifiedWords)")
    suspend fun getFrequency(simplifiedWords: List<String>): List<ChineseWordFrequency>

    suspend fun getFrequencyMapped(simplifiedWords: List<String>): Map<String, ChineseWordFrequency> {
        val current = getFrequency(simplifiedWords)
        return current.associateBy { it.simplified }
    }

    @Insert
    suspend fun insertAll(annotations: List<ChineseWordFrequency>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(freq: ChineseWordFrequency)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(freq: List<ChineseWordFrequency>)

    @Query("DELETE FROM chinesewordfrequency")
    suspend fun deleteAll(): Int
}