package fr.berliat.hskwidget.data.dao

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert
import fr.berliat.hskwidget.data.model.ChineseWord

data class WordFieldsStats(
    val total: Int,
    val collocationsCnt: Int,
    val examplesCnt: Int,
    val antonymsAndSynonymsCnt: Int,
    val typeAndUsageCnt: Int
)

@Dao
interface ChineseWordDAO {
    @Upsert
    suspend fun upsertAll(items: List<ChineseWord>)

    @Query("SELECT * FROM chinese_word")
    suspend fun getAll(): List<ChineseWord>

    @Query("SELECT simplified FROM chinese_word")
    suspend fun getAllSimplifiedWords(): List<String>

    @Query("SELECT * FROM chinese_word WHERE simplified = :simplifiedWord")
    suspend fun findWordFromSimplified(simplifiedWord: String?): ChineseWord?

    @Query("SELECT COUNT(*) FROM chinese_word")
    suspend fun getCount(): Int

    @Query("""
        SELECT
            COUNT(*) AS total,
            COALESCE(SUM(CASE WHEN collocations IS NOT NULL AND collocations != '' AND collocations != 'N/A' THEN 1 ELSE 0 END), 0) AS collocationsCnt,
            COALESCE(SUM(CASE WHEN examples IS NOT NULL AND examples != '' AND examples != 'N/A' THEN 1 ELSE 0 END), 0) AS examplesCnt,
            COALESCE(SUM(CASE WHEN antonym IS NOT NULL AND antonym != '' AND synonyms IS NOT NULL AND synonyms != '' THEN 1 ELSE 0 END), 0) AS antonymsAndSynonymsCnt,
            COALESCE(SUM(CASE WHEN type IS NOT NULL AND type != '' AND type != 'N/A' AND modality IS NOT NULL AND modality != '' AND modality != 'N/A' THEN 1 ELSE 0 END), 0) AS typeAndUsageCnt
        FROM chinese_word
    """)
    suspend fun getFieldsStats(): WordFieldsStats

    @Query("DELETE FROM chinese_word")
    suspend fun deleteAll()
}