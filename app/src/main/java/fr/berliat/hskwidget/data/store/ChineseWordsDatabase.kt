package fr.berliat.hskwidget.data.store

import android.content.Context
import android.util.Log

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

import fr.berliat.hskwidget.data.dao.AnnotatedChineseWordDAO
import fr.berliat.hskwidget.data.dao.ChineseWordDAO
import fr.berliat.hskwidget.data.dao.ChineseWordAnnotationDAO
import fr.berliat.hskwidget.data.model.ChineseWord
import fr.berliat.hskwidget.data.model.ChineseWordAnnotation

import java.util.concurrent.Executors

@Database(entities = [ChineseWordAnnotation::class, ChineseWord::class], version = 1, exportSchema = false)
@TypeConverters(ChineseWord.Pinyins::class,
    fr.berliat.hskwidget.data.store.TypeConverters.DateConverter::class,
    fr.berliat.hskwidget.data.store.TypeConverters.DefinitionsConverter::class,
    fr.berliat.hskwidget.data.store.TypeConverters.AnnotatedChineseWordsConverter::class)
abstract class ChineseWordsDatabase : RoomDatabase() {
    abstract fun annotatedChineseWordDAO(): AnnotatedChineseWordDAO
    abstract fun chineseWordAnnotationDAO(): ChineseWordAnnotationDAO
    abstract fun chineseWordDAO(): ChineseWordDAO

    companion object {
        //Todo: Monitor for memory leak.
        @Volatile
        private var INSTANCE: ChineseWordsDatabase? = null

        fun getInstance(context: Context): ChineseWordsDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }

        private fun buildDatabase(context: Context) =
            Room.databaseBuilder(context.applicationContext,
                    ChineseWordsDatabase::class.java, "chinese_words.db")
                .createFromAsset("databases/chinese_words.db")
                .setQueryCallback(RoomDatabase.QueryCallback { sqlQuery, bindArgs ->
                    Log.d("ChineseWordsDatabase", "SQL Query: $sqlQuery SQL Args: $bindArgs")
                }, Executors.newSingleThreadExecutor())
                .build()
    }
}