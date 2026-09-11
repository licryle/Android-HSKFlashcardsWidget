package fr.berliat.hskwidget.data.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import fr.berliat.hskwidget.data.model.WordDefinition

@Dao
interface WordDefinitionDAO {
    @Upsert
    suspend fun upsertAll(items: List<WordDefinition>)

    @Query("SELECT * FROM word_definition")
    suspend fun getAll(): List<WordDefinition>

    @Query("SELECT * FROM word_definition WHERE simplified IN (:simplifiedWords)")
    suspend fun getForWords(simplifiedWords: List<String>): List<WordDefinition>
}
