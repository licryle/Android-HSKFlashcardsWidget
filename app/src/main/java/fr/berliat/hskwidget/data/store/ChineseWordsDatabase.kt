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
import fr.berliat.hskwidget.data.dao.ChineseWordAnnotationDAO
import fr.berliat.hskwidget.data.dao.ChineseWordDAO
import fr.berliat.hskwidget.data.dao.ChineseWordFrequencyDAO
import fr.berliat.hskwidget.data.dao.WordListDAO
import fr.berliat.hskwidget.data.model.ChineseWord
import fr.berliat.hskwidget.data.model.ChineseWordAnnotation
import fr.berliat.hskwidget.data.model.ChineseWordFrequency
import fr.berliat.hskwidget.data.model.WordList
import fr.berliat.hskwidget.data.model.WordListEntry
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.time.Instant
import java.util.concurrent.Executors


private fun SupportSQLiteDatabase.hasAlreadyDoneMigration(migration: Migration): Boolean {
    return version >= migration.endVersion
}

@Database(
    entities = [ChineseWordAnnotation::class, ChineseWord::class, ChineseWordFrequency::class, WordList::class, WordListEntry::class],
    version = 9, exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2)
    ])
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

        @Volatile
        private var INSTANCE: ChineseWordsDatabase? = null
        private val mutex = Mutex()

        const val DATABASE_FILE = "chinese_words.db"

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                if (db.hasAlreadyDoneMigration(this)) {
                    return
                }
                Log.d(TAG, "Migrating database from version 2 to version 3 (adding anki_id column")
                try {
                    // Add a new column or any other complex changes for version 3
                    db.execSQL("ALTER TABLE ChineseWordAnnotation ADD COLUMN anki_id INTEGER NOT NULL DEFAULT 0")
                    db.execSQL("ALTER TABLE AnnotatedChineseWord ADD COLUMN anki_id INTEGER NOT NULL DEFAULT 0")
                } catch (e: Exception) {
                    Log.d(TAG, "Room bullshit.")
                }
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                if (db.hasAlreadyDoneMigration(this)) {
                    return
                }
                Log.d(TAG, "Migrating database from version 3 to version 4 (adding word lists)")
                try {
                    // Create word_lists table
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS word_lists (
                            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            name TEXT NOT NULL,
                            creationDate INTEGER NOT NULL
                        )
                    """
                    )

                    // Create word_list_entries table with correct schema
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS word_list_entries (
                            listId INTEGER NOT NULL,
                            wordId TEXT NOT NULL,
                            PRIMARY KEY(listId, wordId),
                            FOREIGN KEY(listId) REFERENCES word_lists(id) ON DELETE CASCADE,
                            FOREIGN KEY(wordId) REFERENCES ChineseWord(simplified) ON DELETE CASCADE
                        )
                    """
                    )

                    // Create indices
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_word_list_entries_listId ON word_list_entries(listId)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_word_list_entries_wordId ON word_list_entries(wordId)")
                } catch (e: Exception) {
                    Log.d(TAG, "Error during migration: ${e.message}")
                }
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val now = Instant.now().toEpochMilli()
                db.execSQL("ALTER TABLE word_lists ADD COLUMN lastModified INTEGER NOT NULL DEFAULT $now")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // WordList
                db.execSQL("ALTER TABLE word_lists ADD COLUMN ankiDeckId INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE word_lists ADD COLUMN listType TEXT NOT NULL DEFAULT 'user'")
                val stmt = db.compileStatement(
                    """
                    INSERT INTO word_lists 
                        (name, listType, creationDate, lastModified, ankiDeckId) 
                    VALUES (?, ?, ?, ?, ?)
                """.trimIndent()
                )

                val currentTime = Instant.now().toEpochMilli()
                stmt.bindString(1, WordList.SYSTEM_ANNOTATED_NAME)
                stmt.bindString(2, "SYSTEM")
                stmt.bindLong(3, currentTime)
                stmt.bindLong(4, currentTime)
                stmt.bindLong(5, 0)

                val insertedId = stmt.executeInsert()  // ✅ This gives you the generated ID

                // Word List entries
                db.execSQL(
                    """
                    CREATE TABLE word_list_entries_new (
                        listId INTEGER NOT NULL,
                        simplified TEXT NOT NULL,
                        ankiNoteId INTEGER NOT NULL,
                        PRIMARY KEY(listId, simplified, ankiNoteId),
                        FOREIGN KEY(listId) REFERENCES word_lists(id) ON DELETE CASCADE,
                        FOREIGN KEY(simplified) REFERENCES ChineseWord(simplified) ON DELETE CASCADE
                    )
                """.trimIndent()
                )

                // Step 3: Copy data
                db.execSQL(
                    """
                    INSERT INTO word_list_entries_new (listId, simplified, ankiNoteId)
                    SELECT listId, wordId, ${WordList.ANKI_ID_EMPTY} FROM word_list_entries
                """
                )

                // Step 4: Drop old table, rename new
                db.execSQL("DROP TABLE word_list_entries")
                db.execSQL("ALTER TABLE word_list_entries_new RENAME TO word_list_entries")

                // Step 5: Recreate indices
                db.execSQL("CREATE INDEX index_word_list_entries_listId ON word_list_entries(listId)")
                db.execSQL("CREATE INDEX index_word_list_entries_simplified ON word_list_entries(simplified)")
                db.execSQL("CREATE INDEX index_word_list_entries_ankiNoteId ON word_list_entries(ankiNoteId)")

                db.execSQL(
                    """INSERT INTO word_list_entries (listId, simplified, ankiNoteId)
                                            SELECT $insertedId, a_simplified, anki_id FROM ChineseWordAnnotation"""
                )

                // Removing ankiId from Annotations
                db.execSQL(
                    """
                    CREATE TABLE ChineseWordAnnotation_new (
                        a_simplified TEXT NOT NULL,
                        a_pinyins TEXT,
                        notes TEXT,
                        class_type TEXT,
                        class_level TEXT,
                        themes TEXT,
                        first_seen INTEGER,
                        a_searchable_text TEXT NOT NULL DEFAULT '',
                        is_exam INTEGER,
                        PRIMARY KEY(a_simplified)
                    )
                """.trimIndent()
                )

                db.execSQL(
                    """
                    INSERT INTO ChineseWordAnnotation_new (
                        a_simplified,
                        a_pinyins,
                        notes,
                        class_type,
                        class_level,
                        themes,
                        first_seen,
                        a_searchable_text,
                        is_exam
                    )
                    SELECT
                        a_simplified,
                        a_pinyins,
                        notes,
                        class_type,
                        class_level,
                        themes,
                        first_seen,
                        a_searchable_text,
                        is_exam
                    FROM ChineseWordAnnotation
                """.trimIndent()
                )

                db.execSQL("DROP TABLE ChineseWordAnnotation")
                db.execSQL("ALTER TABLE ChineseWordAnnotation_new RENAME TO ChineseWordAnnotation")

                db.execSQL(
                    """
                    CREATE INDEX index_ChineseWordAnnotation_a_searchable_text 
                    ON ChineseWordAnnotation(a_searchable_text)
                """.trimIndent()
                )
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE word_list_entries_new (
                        listId INTEGER NOT NULL,
                        simplified TEXT NOT NULL,
                        ankiNoteId INTEGER NOT NULL,
                        PRIMARY KEY (listId, simplified, ankiNoteId),
                        FOREIGN KEY (listId) REFERENCES word_lists(id) ON DELETE CASCADE
                    )
                """.trimIndent()
                )

                db.execSQL(
                    """
                    INSERT INTO word_list_entries_new (listId, simplified, ankiNoteId)
                    SELECT listId, simplified, ankiNoteId FROM word_list_entries
                """.trimIndent()
                )

                db.execSQL("DROP TABLE word_list_entries")
                db.execSQL("ALTER TABLE word_list_entries_new RENAME TO word_list_entries")

                db.execSQL("CREATE INDEX IF NOT EXISTS index_word_list_entries_listId ON word_list_entries(listId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_word_list_entries_simplified ON word_list_entries(simplified)")
            }
        }

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE ChineseWord ADD COLUMN modality TEXT DEFAULT 'N/A' CHECK(modality IN ('ORAL', 'WRITTEN', 'ORAL_WRITTEN', 'N/A'))")
                db.execSQL("ALTER TABLE ChineseWord ADD COLUMN examples TEXT DEFAULT ''")
                db.execSQL("ALTER TABLE ChineseWord ADD COLUMN type TEXT DEFAULT 'N/A' CHECK(type IN ('NOUN', 'VERB', 'ADJECTIVE', 'ADVERB', 'CONJUNCTION', 'PREPOSITION', 'INTERJECTION', 'IDIOM', 'N/A'))")
                db.execSQL("ALTER TABLE ChineseWord ADD COLUMN synonyms TEXT DEFAULT ''")
                db.execSQL("ALTER TABLE ChineseWord ADD COLUMN antonym TEXT DEFAULT ''")
            }
        }

        private fun replaceChineseWordMigration(
            fromVersion: Int,
            toVersion: Int,
            context: Context
        ) =
            object : ReplaceTablesFromAssetMigration(fromVersion, toVersion, context) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    replaceTables(db, listOf("ChineseWord"))
                }
            }

        private fun buildDatabase(context: Context) =
            Room.databaseBuilder(
                context.applicationContext,
                ChineseWordsDatabase::class.java, DATABASE_FILE
            )
                .createFromAsset("databases/$DATABASE_FILE")
                .setQueryCallback({ sqlQuery, bindArgs ->
                    Log.d(TAG, "SQL Query: $sqlQuery SQL Args: $bindArgs")
                }, Executors.newSingleThreadExecutor())
                .addMigrations(
                    MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6,
                    MIGRATION_6_7, MIGRATION_7_8,
                    replaceChineseWordMigration(8, 9, context)
                )
                .build()

        fun loadExternalDatabase(context: Context, dbFile: File) =
            Room.databaseBuilder(
                context.applicationContext,
                ChineseWordsDatabase::class.java,
                dbFile.path
            )
                .addMigrations(
                    MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6,
                    MIGRATION_6_7, MIGRATION_7_8,
                    replaceChineseWordMigration(8, 9, context)
                )
                .createFromFile(dbFile) // instead of fromAsset
                .build()
    }
}