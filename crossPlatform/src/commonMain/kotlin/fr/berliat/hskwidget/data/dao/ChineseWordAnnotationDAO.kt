package fr.berliat.hskwidget.data.dao

import androidx.room3.Dao
import androidx.room3.Delete
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import fr.berliat.hskwidget.data.model.ChineseWordAnnotation

@Dao
interface ChineseWordAnnotationDAO {
    @Query("SELECT * FROM chinese_word_annotation")
    suspend fun getAll(): List<ChineseWordAnnotation>

    @Query("SELECT a_simplified FROM chinese_word_annotation")
    suspend fun getAllSimplifiedAnnotations(): List<String>

    @Query("SELECT * FROM chinese_word_annotation WHERE " +
            "a_simplified = :simplified LIMIT 1")
    suspend fun findBySimplified(simplified: String): ChineseWordAnnotation

    @Insert
    suspend fun insertAll(annotations: List<ChineseWordAnnotation>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(annotation: ChineseWordAnnotation)

    @Delete
    suspend fun delete(annotation: ChineseWordAnnotation)

    @Query("DELETE FROM chinese_word_annotation WHERE a_simplified = :simplified")
    suspend fun deleteBySimplified(simplified: String): Int

    @Query("DELETE FROM chinese_word_annotation")
    suspend fun deleteAll(): Int

    @Query("SELECT COUNT(*) FROM chinese_word_annotation")
    suspend fun getCount(): Int
}