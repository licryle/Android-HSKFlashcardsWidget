package fr.berliat.hskwidget.data.store

import android.content.Context
import android.util.Log
import androidx.room.AutoMigration

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

import fr.berliat.hskwidget.data.dao.AnnotatedChineseWordDAO
import fr.berliat.hskwidget.data.dao.ChineseWordDAO
import fr.berliat.hskwidget.data.dao.ChineseWordAnnotationDAO
import fr.berliat.hskwidget.data.dao.ChineseWordFrequencyDAO
import fr.berliat.hskwidget.data.dao.WordListDAO
import fr.berliat.hskwidget.data.model.ChineseWord
import fr.berliat.hskwidget.data.model.ChineseWordAnnotation
import fr.berliat.hskwidget.data.model.ChineseWordFrequency
import fr.berliat.hskwidget.data.model.WordList
import fr.berliat.hskwidget.data.model.WordListEntry
import java.io.File

import java.util.concurrent.Executors


private fun SupportSQLiteDatabase.hasAlreadyDoneMigration(migration: Migration): Boolean {
    return version >= migration.endVersion
}

@Database(
    entities = [ChineseWordAnnotation::class, ChineseWord::class, ChineseWordFrequency::class, WordList::class, WordListEntry::class],
    version = 5, exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2)
    ])
@TypeConverters(ChineseWord.Pinyins::class,
    fr.berliat.hskwidget.data.store.TypeConverters.DateConverter::class,
    fr.berliat.hskwidget.data.store.TypeConverters.DefinitionsConverter::class,
    fr.berliat.hskwidget.data.store.TypeConverters.AnnotatedChineseWordsConverter::class)

abstract class ChineseWordsDatabase : RoomDatabase() {
    abstract fun annotatedChineseWordDAO(): AnnotatedChineseWordDAO
    abstract fun chineseWordAnnotationDAO(): ChineseWordAnnotationDAO
    abstract fun chineseWordDAO(): ChineseWordDAO
    abstract fun chineseWordFrequencyDAO(): ChineseWordFrequencyDAO
    abstract fun wordListDAO(): WordListDAO

    companion object {
        const val TAG = "ChineseWordsDatabase"

        //Todo: Monitor for memory leak.
        @Volatile
        private var INSTANCE: ChineseWordsDatabase? = null

        const val DATABASE_FILE = "chinese_words.db"

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                if (database.hasAlreadyDoneMigration(this)) {
                    return
                }
                Log.d(TAG, "Migrating database from version 2 to version 3 (adding anki_id column")
                try {
                    // Add a new column or any other complex changes for version 3
                    database.execSQL("ALTER TABLE ChineseWordAnnotation ADD COLUMN anki_id INTEGER NOT NULL DEFAULT " + ChineseWordAnnotation.ANKI_ID_EMPTY)
                    database.execSQL("ALTER TABLE AnnotatedChineseWord ADD COLUMN anki_id INTEGER NOT NULL DEFAULT " + ChineseWordAnnotation.ANKI_ID_EMPTY)
                } catch (e: Exception) {
                    Log.d(TAG, "Room bullshit.")
                }
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                if (database.hasAlreadyDoneMigration(this)) {
                    return
                }
                Log.d(TAG, "Migrating database from version 3 to version 4 (adding word lists)")
                try {
                    // Create word_lists table
                    database.execSQL("""
                        CREATE TABLE IF NOT EXISTS word_lists (
                            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            name TEXT NOT NULL,
                            creationDate INTEGER NOT NULL
                        )
                    """)
                    
                    // Create word_list_entries table with correct schema
                    database.execSQL("""
                        CREATE TABLE IF NOT EXISTS word_list_entries (
                            listId INTEGER NOT NULL,
                            wordId TEXT NOT NULL,
                            PRIMARY KEY(listId, wordId),
                            FOREIGN KEY(listId) REFERENCES word_lists(id) ON DELETE CASCADE,
                            FOREIGN KEY(wordId) REFERENCES ChineseWord(simplified) ON DELETE CASCADE
                        )
                    """)

                    // Create indices
                    database.execSQL("CREATE INDEX IF NOT EXISTS index_word_list_entries_listId ON word_list_entries(listId)")
                    database.execSQL("CREATE INDEX IF NOT EXISTS index_word_list_entries_wordId ON word_list_entries(wordId)")
                } catch (e: Exception) {
                    Log.d(TAG, "Error during migration: ${e.message}")
                }
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE word_lists ADD COLUMN lastModified INTEGER NOT NULL DEFAULT ${System.currentTimeMillis()}")
            }
        }

        fun getInstance(context: Context): ChineseWordsDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }

        private fun buildDatabase(context: Context) =
            Room.databaseBuilder(context.applicationContext,
                    ChineseWordsDatabase::class.java, DATABASE_FILE)
                .createFromAsset("databases/$DATABASE_FILE")
                .setQueryCallback({ sqlQuery, bindArgs ->
                    Log.d(TAG, "SQL Query: $sqlQuery SQL Args: $bindArgs")
                }, Executors.newSingleThreadExecutor())
                .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                .build()

        fun loadExternalDatabase(context: Context, dbFile: File) = Room.databaseBuilder(
                context.applicationContext,
                ChineseWordsDatabase::class.java,
                dbFile.path
            )
            .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5) // Add migration logic
            .createFromFile(dbFile) // instead of fromAsset
            .build()
    }
}