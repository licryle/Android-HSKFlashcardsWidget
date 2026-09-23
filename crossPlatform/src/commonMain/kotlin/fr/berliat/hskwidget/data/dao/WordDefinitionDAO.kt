package fr.berliat.hskwidget.data.dao

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert
import fr.berliat.hskwidget.data.model.WordDefinition

data class DefinitionLanguageStats(
    val englishCnt: Int,
    val frenchCnt: Int,
    val hsk3Cnt: Int
)

@Dao
interface WordDefinitionDAO {
    @Upsert
    suspend fun upsertAll(items: List<WordDefinition>)

    @Query("SELECT * FROM word_definition")
    suspend fun getAll(): List<WordDefinition>

    @Query("SELECT * FROM word_definition WHERE simplified IN (:simplifiedWords)")
    suspend fun getForWords(simplifiedWords: List<String>): List<WordDefinition>

    @Query("SELECT COUNT(*) FROM word_definition")
    suspend fun getCount(): Int

    @Query("""
        SELECT
            COALESCE(SUM(CASE WHEN language = :enCode AND definition IS NOT NULL AND definition != '' THEN 1 ELSE 0 END), 0) AS englishCnt,
            COALESCE(SUM(CASE WHEN language = :frCode AND definition IS NOT NULL AND definition != '' THEN 1 ELSE 0 END), 0) AS frenchCnt,
            COALESCE(SUM(CASE WHEN language = :hsk3Code AND definition IS NOT NULL AND definition != '' THEN 1 ELSE 0 END), 0) AS hsk3Cnt
        FROM word_definition
    """)
    suspend fun getLanguageStats(
        enCode: String = "en",
        frCode: String = "fr",
        hsk3Code: String = "zh-CN-HSK03"
    ): DefinitionLanguageStats

    @Query("DELETE FROM word_definition")
    suspend fun deleteAll()
}
