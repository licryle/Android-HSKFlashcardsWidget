package fr.berliat.hskwidget.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Delete
import androidx.room.OnConflictStrategy
import androidx.room.Query
import fr.berliat.hskwidget.data.model.ChineseWordAnnotation

@Dao
interface ChineseWordAnnotationDAO {
    @Query("SELECT * FROM chinesewordannotation")
    fun getAll(): List<ChineseWordAnnotation>

    @Query("SELECT * FROM chinesewordannotation WHERE " +
            "a_simplified = :simplified LIMIT 1")
    fun findBySimplified(simplified: String): ChineseWordAnnotation

    @Insert
    fun _insertAll(vararg annotations: ChineseWordAnnotation)

    fun insertAll(annotations: Array<ChineseWordAnnotation>) {
        annotations.forEach {
            it.updateSearchable()
        }

        _insertAll(*annotations)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun _insertOrUpdate(annotation: ChineseWordAnnotation)

    suspend fun insertOrUpdate(annotation: ChineseWordAnnotation) {
        annotation.updateSearchable()
        _insertOrUpdate(annotation)
    }

    @Delete
    fun delete(annotation: ChineseWordAnnotation)

    @Query("DELETE FROM chinesewordannotation WHERE a_simplified = :simplified")
    fun deleteBySimplified(simplified: String): Int
}