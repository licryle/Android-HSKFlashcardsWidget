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
    fun insertAll(vararg users: ChineseWordAnnotation)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(annotation: ChineseWordAnnotation)

    @Delete
    fun delete(annotation: ChineseWordAnnotation)

    @Query("DELETE FROM chinesewordannotation WHERE a_simplified = :simplified")
    fun deleteBySimplified(simplified: String): Int
}