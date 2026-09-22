package fr.berliat.hskwidget.data.dao

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert
import fr.berliat.hskwidget.data.model.WordDefinition

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

    @Query("DELETE FROM word_definition")
    suspend fun deleteAll()
}
