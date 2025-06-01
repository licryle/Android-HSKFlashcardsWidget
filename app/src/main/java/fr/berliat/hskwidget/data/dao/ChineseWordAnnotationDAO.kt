package fr.berliat.hskwidget.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Delete
import androidx.room.OnConflictStrategy
import androidx.room.Query
import fr.berliat.hskwidget.data.model.ChineseWordAnnotation

@Dao
interface ChineseWordAnnotationDAO {
    @Query("SELECT * FROM chinese_word_annotation")
    suspend fun getAll(): List<ChineseWordAnnotation>

    @Query("SELECT * FROM chinese_word_annotation WHERE " +
            "a_simplified = :simplified LIMIT 1")
    suspend fun findBySimplified(simplified: String): ChineseWordAnnotation

    @Insert
    suspend fun _insertAll(annotations: List<ChineseWordAnnotation>)

    suspend fun insertAll(annotations: List<ChineseWordAnnotation>) {
        annotations.forEach {
            it.updateSearchable()
        }

        _insertAll(annotations)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun _insertOrUpdate(annotation: ChineseWordAnnotation)

    suspend fun insertOrUpdate(annotation: ChineseWordAnnotation) {
        annotation.updateSearchable()
        _insertOrUpdate(annotation)
    }

    @Delete
    suspend fun delete(annotation: ChineseWordAnnotation)

    @Query("DELETE FROM chinese_word_annotation WHERE a_simplified = :simplified")
    suspend fun deleteBySimplified(simplified: String): Int

    @Query("DELETE FROM chinese_word_annotation")
    suspend fun deleteAll(): Int

    @Query("SELECT COUNT(*) FROM chinese_word_annotation")
    suspend fun getCount(): Int
}