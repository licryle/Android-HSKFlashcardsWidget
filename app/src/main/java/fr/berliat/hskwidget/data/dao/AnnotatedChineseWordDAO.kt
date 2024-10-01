package fr.berliat.hskwidget.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Delete
import androidx.room.Query
import fr.berliat.hskwidget.data.model.AnnotatedChineseWord

@Dao
interface AnnotatedChineseWordDAO {
    @Query("SELECT * FROM annotatedchineseword")
    fun getAll(): List<AnnotatedChineseWord>

    @Query("SELECT * FROM annotatedchineseword WHERE " +
            "simplified = :simplified LIMIT 1")
    fun findBySimplified(simplified: String): AnnotatedChineseWord

    @Insert
    fun insertAll(vararg users: AnnotatedChineseWord)

    @Delete
    fun delete(user: AnnotatedChineseWord)
}