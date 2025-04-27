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
import fr.berliat.hskwidget.data.model.ChineseWord
import fr.berliat.hskwidget.data.model.ChineseWordAnnotation
import fr.berliat.hskwidget.data.model.ChineseWordFrequency
import java.io.File

import java.util.concurrent.Executors


private fun SupportSQLiteDatabase.hasAlreadyDoneMigration(migration: Migration): Boolean {
    return version >= migration.endVersion
}

@Database(
    entities = [ChineseWordAnnotation::class, ChineseWord::class, ChineseWordFrequency::class],
    version = 3, exportSchema = true,
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

    companion object {
        //Todo: Monitor for memory leak.
        @Volatile
        private var INSTANCE: ChineseWordsDatabase? = null

        const val DATABASE_FILE = "chinese_words.db"

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                if (database.hasAlreadyDoneMigration(this)) {
                    return
                }
                Log.d("ChineseWordsDatabase", "Migrating database from version 2 to version 3 (adding anki_id column")
                try {
                    // Add a new column or any other complex changes for version 3
                    database.execSQL("ALTER TABLE ChineseWordAnnotation ADD COLUMN anki_id INTEGER NOT NULL DEFAULT " + ChineseWordAnnotation.ANKI_ID_EMPTY)
                    database.execSQL("ALTER TABLE AnnotatedChineseWord ADD COLUMN anki_id INTEGER NOT NULL DEFAULT " + ChineseWordAnnotation.ANKI_ID_EMPTY)
                } catch (e: Exception) {
                    Log.d("ChineseWordsDatabase", "Room bullshit.")
                }
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
                    Log.d("ChineseWordsDatabase", "SQL Query: $sqlQuery SQL Args: $bindArgs")
                }, Executors.newSingleThreadExecutor())
                .addMigrations(MIGRATION_2_3)
                .build()

        fun loadExternalDatabase(context: Context, dbFile: File) = Room.databaseBuilder(
                context.applicationContext,
                ChineseWordsDatabase::class.java,
                dbFile.path
            )
            .addMigrations(MIGRATION_2_3) // Add migration logic
            .createFromFile(dbFile) // instead of fromAsset
            .build()
    }
}