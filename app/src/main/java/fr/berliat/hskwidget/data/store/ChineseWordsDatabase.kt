package fr.berliat.hskwidget.data.store

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import fr.berliat.hskwidget.data.dao.AnnotatedChineseWordDAO
import fr.berliat.hskwidget.data.dao.ChineseWordAnnotationDAO
import fr.berliat.hskwidget.data.dao.ChineseWordDAO
import fr.berliat.hskwidget.data.dao.ChineseWordFrequencyDAO
import fr.berliat.hskwidget.data.dao.WidgetListDAO
import fr.berliat.hskwidget.data.dao.WordListDAO
import fr.berliat.hskwidget.data.model.ChineseWord
import fr.berliat.hskwidget.data.model.ChineseWordAnnotation
import fr.berliat.hskwidget.data.model.ChineseWordFrequency
import fr.berliat.hskwidget.data.model.WidgetListEntry
import fr.berliat.hskwidget.data.model.WordList
import fr.berliat.hskwidget.data.model.WordListEntry
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.util.concurrent.Executors

@Database(
    entities = [ChineseWordAnnotation::class, ChineseWord::class, ChineseWordFrequency::class,
                WordList::class, WordListEntry::class, WidgetListEntry::class],
    version = 1, exportSchema = true)
@TypeConverters(ChineseWord.Pinyins::class,
    WordTypeConverter::class,
    ModalityConverter::class,
    DateConverter::class,
    DefinitionsConverter::class,
    AnnotatedChineseWordsConverter::class)

abstract class ChineseWordsDatabase : RoomDatabase() {
    abstract fun annotatedChineseWordDAO(): AnnotatedChineseWordDAO
    abstract fun chineseWordAnnotationDAO(): ChineseWordAnnotationDAO
    abstract fun chineseWordDAO(): ChineseWordDAO
    abstract fun chineseWordFrequencyDAO(): ChineseWordFrequencyDAO
    abstract fun wordListDAO(): WordListDAO
    abstract fun widgetListDAO(): WidgetListDAO

    companion object {
        suspend fun getInstance(context: Context): ChineseWordsDatabase {
            INSTANCE?.let { return it }

            return mutex.withLock {
                INSTANCE?.let { return it }

                val instance = buildDatabase(context)

                INSTANCE = instance
                instance
            }
        }

        const val TAG = "ChineseWordsDatabase"
        const val SQL_VAR_MAX = 900

        @Volatile
        private var INSTANCE: ChineseWordsDatabase? = null
        private val mutex = Mutex()

        const val DATABASE_FILE = "chinese_words.db"

        private fun buildDatabase(context: Context) =
            Room.databaseBuilder(
                context.applicationContext,
                ChineseWordsDatabase::class.java, DATABASE_FILE
            )
                .createFromAsset("databases/$DATABASE_FILE")
                .setQueryCallback({ sqlQuery, bindArgs ->
                    Log.d(TAG, "SQL Query: $sqlQuery SQL Args: $bindArgs")
                }, Executors.newSingleThreadExecutor())
                .build()

        fun loadExternalDatabase(context: Context, dbFile: File) =
            Room.databaseBuilder(
                context.applicationContext,
                ChineseWordsDatabase::class.java,
                dbFile.path
            )
                .createFromFile(dbFile) // instead of fromAsset
                .build()
    }
}