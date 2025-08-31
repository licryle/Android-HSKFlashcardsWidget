package fr.berliat.hskwidget.data.store

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import fr.berliat.hskwidget.BuildConfig
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
    AnnotatedChineseWordsConverter::class,
    ListTypeConverter::class)

abstract class ChineseWordsDatabase: RoomDatabase() {
    abstract fun annotatedChineseWordDAO(): AnnotatedChineseWordDAO
    abstract fun chineseWordAnnotationDAO(): ChineseWordAnnotationDAO
    abstract fun chineseWordDAO(): ChineseWordDAO
    abstract fun chineseWordFrequencyDAO(): ChineseWordFrequencyDAO
    abstract fun wordListDAO(): WordListDAO
    abstract fun widgetListDAO(): WidgetListDAO

    val databasePath
        get() = openHelper.writableDatabase.path.toString()

    companion object {
        const val TAG = "ChineseWordsDatabase"

        suspend fun createInstance(context: Context) : ChineseWordsDatabase {
            val dbBuilder = Room.databaseBuilder(
                context.applicationContext,
                ChineseWordsDatabase::class.java,
                DatabaseHelper.DATABASE_FILENAME
            )
                .createFromAsset(DatabaseHelper.DATABASE_ASSET_PATH)

            if (BuildConfig.DEBUG) {
                dbBuilder.setQueryCallback(
                    { sqlQuery, bindArgs ->
                        Log.d(TAG, "SQL Query: $sqlQuery SQL Args: $bindArgs")
                    }, Executors.newSingleThreadExecutor()
                )
            }

            return dbBuilder.build()
        }
    }

    fun flushToDisk() {
        val cur = query("PRAGMA wal_checkpoint(FULL)", null)
        cur.moveToFirst()
        cur.close()
    }
}