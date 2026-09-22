package fr.berliat.hskwidget.domain

import androidx.room3.RoomDatabase
import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL

import co.touchlab.kermit.Logger
import fr.berliat.hskwidget.Res

import fr.berliat.hskwidget.core.Utils
import fr.berliat.hskwidget.core.AppDispatchers
import fr.berliat.hskwidget.core.HSKAppServices
import fr.berliat.hskwidget.core.Logging
import fr.berliat.hskwidget.core.SnackbarType
import fr.berliat.hskwidget.data.store.ChineseWordsDatabase
import fr.berliat.hskwidget.database_update_failure
import fr.berliat.hskwidget.database_update_start
import fr.berliat.hskwidget.database_update_success

import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.absolutePath
import io.github.vinceglb.filekit.cacheDir
import io.github.vinceglb.filekit.copyTo
import io.github.vinceglb.filekit.delete
import io.github.vinceglb.filekit.div
import io.github.vinceglb.filekit.exists
import io.github.vinceglb.filekit.list
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.path

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

data class DatabaseBuilderWithPath(
    val file: PlatformFile,
    val builder: RoomDatabase.Builder<ChineseWordsDatabase>
)

class DatabaseUpdatingException(message: String) : Exception(message)

class DatabaseHelper private constructor() {
    val liveDatabase: ChineseWordsDatabase
        get() {
            if (updateProgress.value != null) throw DatabaseUpdatingException("Database is currently being updated from asset.")
            return _db!!
        }

    private var _db: ChineseWordsDatabase? = null
    lateinit var DATABASE_LIVE_PATH : PlatformFile
        private set
    lateinit var DATABASE_LIVE_DIR : PlatformFile
        private set

    companion object {
        private val _updateProgress = MutableStateFlow<Float?>(null)
        val updateProgress = _updateProgress.asStateFlow()

        private var INSTANCE: DatabaseHelper? = null
        private val mutex = Mutex()
        const val DATABASE_FILENAME = "Mandarin_Assistant.db"
        const val DATABASE_ASSET_PATH = "databases/$DATABASE_FILENAME"
        private const val TAG = "ChineseWordsDatabase"
        const val TEMP_FILE_PREFIX = "DB_TMP_"

        fun getDatabaseLiveDir() = Utils.getAppDatabasePath()
        fun getDatabaseLiveFile() = getDatabaseLiveDir() / DATABASE_FILENAME

        suspend fun getInstance(): DatabaseHelper = withContext(AppDispatchers.IO) {
            INSTANCE?.let { return@withContext it } // for optimization

            mutex.withLock {
                INSTANCE?.let { return@withContext it } // for safety

                val instance = DatabaseHelper()
                instance._db = createRoomDatabaseLive()
                instance.DATABASE_LIVE_DIR = getDatabaseLiveDir()
                instance.DATABASE_LIVE_PATH = getDatabaseLiveFile()

                // safely assign singleton
                INSTANCE = INSTANCE ?: instance
            }

            return@withContext INSTANCE!!
        }

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override suspend fun migrate(connection: SQLiteConnection) {
                connection.execSQL("ALTER TABLE chinese_word ADD COLUMN collocations TEXT DEFAULT ''")
            }
        }

        /**
         * Dictionary data is supplied by the versioned asset.  Definitions are deliberately
         * not migrated from the old bundled dictionary: only user-owned tables are retained.
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override suspend fun migrate(connection: SQLiteConnection) {
                connection.execSQL("""
                    CREATE TABLE IF NOT EXISTS `chinese_word_new` (
                        `simplified` TEXT NOT NULL,
                        `traditional` TEXT,
                        `hsk_level` TEXT,
                        `pinyins` TEXT,
                        `popularity` INTEGER,
                        `examples` TEXT DEFAULT '',
                        `collocations` TEXT DEFAULT '',
                        `modality` TEXT DEFAULT 'N/A',
                        `type` TEXT DEFAULT 'N/A',
                        `synonyms` TEXT DEFAULT '',
                        `antonym` TEXT DEFAULT '',
                        `searchable_text` TEXT NOT NULL DEFAULT '',
                        PRIMARY KEY(`simplified`)
                    )
                """.trimIndent())
                connection.execSQL("""
                    INSERT INTO `chinese_word_new`
                    (`simplified`, `traditional`, `hsk_level`, `pinyins`, `popularity`, `examples`, `collocations`, `modality`, `type`, `synonyms`, `antonym`, `searchable_text`)
                    SELECT `simplified`, `traditional`, `hsk_level`, `pinyins`, `popularity`, `examples`, `collocations`, `modality`, `type`, `synonyms`, `antonym`, ''
                    FROM `chinese_word`
                """.trimIndent())
                connection.execSQL("DROP TABLE `chinese_word`")
                connection.execSQL("ALTER TABLE `chinese_word_new` RENAME TO `chinese_word`")

                connection.execSQL("""
                    CREATE TABLE IF NOT EXISTS `chinese_word_annotation_new` (
                        `a_simplified` TEXT NOT NULL,
                        `a_pinyins` TEXT,
                        `notes` TEXT,
                        `class_type` TEXT,
                        `class_level` TEXT,
                        `themes` TEXT,
                        `first_seen` INTEGER,
                        `is_exam` INTEGER,
                        `a_searchable_text` TEXT NOT NULL DEFAULT '',
                        PRIMARY KEY(`a_simplified`)
                    )
                """.trimIndent())
                connection.execSQL("""
                    INSERT INTO `chinese_word_annotation_new`
                    (`a_simplified`, `a_pinyins`, `notes`, `class_type`, `class_level`, `themes`, `first_seen`, `is_exam`, `a_searchable_text`)
                    SELECT `a_simplified`, `a_pinyins`, `notes`, `class_type`, `class_level`, `themes`, `first_seen`, `is_exam`, ''
                    FROM `chinese_word_annotation`
                """.trimIndent())
                connection.execSQL("DROP TABLE `chinese_word_annotation`")
                connection.execSQL("ALTER TABLE `chinese_word_annotation_new` RENAME TO `chinese_word_annotation`")

                connection.execSQL("""
                    CREATE TABLE IF NOT EXISTS `word_definition` (
                        `simplified` TEXT NOT NULL,
                        `language` TEXT NOT NULL,
                        `definition` TEXT NOT NULL,
                        PRIMARY KEY(`simplified`, `language`),
                        FOREIGN KEY(`simplified`) REFERENCES `chinese_word`(`simplified`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """.trimIndent())
                connection.execSQL("CREATE INDEX IF NOT EXISTS `index_word_definition_language_definition` ON `word_definition` (`language`, `definition`)")

                // Recreate FTS tables and triggers to ensure they are linked to the new content tables
                connection.execSQL("DROP TABLE IF EXISTS `chinese_word_fts`")
                connection.execSQL("CREATE VIRTUAL TABLE `chinese_word_fts` USING FTS5(`searchable_text`, `simplified`, `traditional`, tokenize=`unicode61`, content=`chinese_word`)")
                connection.execSQL("INSERT INTO `chinese_word_fts`(`rowid`, `searchable_text`, `simplified`, `traditional`) SELECT `rowid`, `searchable_text`, `simplified`, `traditional` FROM `chinese_word`")

                connection.execSQL("DROP TABLE IF EXISTS `word_definition_fts`")
                connection.execSQL("CREATE VIRTUAL TABLE `word_definition_fts` USING FTS5(`simplified`, `language`, `definition`, tokenize=`unicode61`, content=`word_definition`)")
                connection.execSQL("INSERT INTO `word_definition_fts`(`rowid`, `simplified`, `language`, `definition`) SELECT `rowid`, `simplified`, `language`, `definition` FROM `word_definition`")

                connection.execSQL("DROP TABLE IF EXISTS `chinese_word_annotation_fts`")
                connection.execSQL("CREATE VIRTUAL TABLE `chinese_word_annotation_fts` USING FTS5(`a_searchable_text`, `a_simplified`, `notes`, `themes`, tokenize=`unicode61`, content=`chinese_word_annotation`)")
                connection.execSQL("INSERT INTO `chinese_word_annotation_fts`(`rowid`, `a_searchable_text`, `a_simplified`, `notes`, `themes`) SELECT `rowid`, `a_searchable_text`, `a_simplified`, `notes`, `themes` FROM `chinese_word_annotation`")

                // Re-register Room's FTS sync triggers
                val triggers = listOf(
                    "CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_chinese_word_fts_BEFORE_UPDATE BEFORE UPDATE ON `chinese_word` BEGIN DELETE FROM `chinese_word_fts` WHERE `rowid`=OLD.`rowid`; END",
                    "CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_chinese_word_fts_BEFORE_DELETE BEFORE DELETE ON `chinese_word` BEGIN DELETE FROM `chinese_word_fts` WHERE `rowid`=OLD.`rowid`; END",
                    "CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_chinese_word_fts_AFTER_UPDATE AFTER UPDATE ON `chinese_word` BEGIN INSERT INTO `chinese_word_fts`(`rowid`, `searchable_text`, `simplified`, `traditional`) VALUES (NEW.`rowid`, NEW.`searchable_text`, NEW.`simplified`, NEW.`traditional`); END",
                    "CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_chinese_word_fts_AFTER_INSERT AFTER INSERT ON `chinese_word` BEGIN INSERT INTO `chinese_word_fts`(`rowid`, `searchable_text`, `simplified`, `traditional`) VALUES (NEW.`rowid`, NEW.`searchable_text`, NEW.`simplified`, NEW.`traditional`); END",
                    "CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_word_definition_fts_BEFORE_UPDATE BEFORE UPDATE ON `word_definition` BEGIN DELETE FROM `word_definition_fts` WHERE `rowid`=OLD.`rowid`; END",
                    "CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_word_definition_fts_BEFORE_DELETE BEFORE DELETE ON `word_definition` BEGIN DELETE FROM `word_definition_fts` WHERE `rowid`=OLD.`rowid`; END",
                    "CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_word_definition_fts_AFTER_UPDATE AFTER UPDATE ON `word_definition` BEGIN INSERT INTO `word_definition_fts`(`rowid`, `simplified`, `language`, `definition`) VALUES (NEW.`rowid`, NEW.`simplified`, NEW.`language`, NEW.`definition`); END",
                    "CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_word_definition_fts_AFTER_INSERT AFTER INSERT ON `word_definition` BEGIN INSERT INTO `word_definition_fts`(`rowid`, `simplified`, `language`, `definition`) VALUES (NEW.`rowid`, NEW.`simplified`, NEW.`language`, NEW.`definition`); END",
                    "CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_chinese_word_annotation_fts_BEFORE_UPDATE BEFORE UPDATE ON `chinese_word_annotation` BEGIN DELETE FROM `chinese_word_annotation_fts` WHERE `rowid`=OLD.`rowid`; END",
                    "CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_chinese_word_annotation_fts_BEFORE_DELETE BEFORE DELETE ON `chinese_word_annotation` BEGIN DELETE FROM `chinese_word_annotation_fts` WHERE `rowid`=OLD.`rowid`; END",
                    "CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_chinese_word_annotation_fts_AFTER_UPDATE AFTER UPDATE ON `chinese_word_annotation` BEGIN INSERT INTO `chinese_word_annotation_fts`(`rowid`, `a_searchable_text`, `a_simplified`, `notes`, `themes`) VALUES (NEW.`rowid`, NEW.`a_searchable_text`, NEW.`a_simplified`, NEW.`notes`, NEW.`themes`); END",
                    "CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_chinese_word_annotation_fts_AFTER_INSERT AFTER INSERT ON `chinese_word_annotation` BEGIN INSERT INTO `chinese_word_annotation_fts`(`rowid`, `a_searchable_text`, `a_simplified`, `notes`, `themes`) VALUES (NEW.`rowid`, NEW.`a_searchable_text`, NEW.`a_simplified`, NEW.`notes`, NEW.`themes`); END"
                )
                triggers.forEach { connection.execSQL(it) }
            }
        }

        private fun buildDatabase(databaseBuilder: DatabaseBuilderWithPath): ChineseWordsDatabase {
            val sqlDriver = BundledSQLiteDriver()
            Logger.d(tag=TAG, messageString = "buildDatabase entering - ${databaseBuilder.file}")
            val finalBuilder = databaseBuilder.builder
                .setDriver(sqlDriver)
                .setQueryCoroutineContext(AppDispatchers.IO)
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)

            val db = finalBuilder.build()
            db._databaseFile = databaseBuilder.file

            Logger.d(tag=TAG, messageString = "buildDatabase exiting - ${databaseBuilder.file}")
            return db
        }

        suspend fun createRoomDatabaseLive() : ChineseWordsDatabase =
            buildDatabase(createRoomDatabaseBuilderLive())

        suspend fun createRoomDatabaseFromFile(file: PlatformFile) : ChineseWordsDatabase =
            buildDatabase(createRoomDatabaseBuilderFromFile(file))

        suspend fun createRoomDatabaseFromAsset() : ChineseWordsDatabase =
            buildDatabase(createRoomDatabaseBuilderFromAsset())

        /** ToDo: could there be a better way to handle DB updates? In AppViewModel it's too late,
         * and here, well, it's in a weird disconnected place.
         */
        private suspend fun createRoomDatabaseBuilderLive(): DatabaseBuilderWithPath = withContext(
            AppDispatchers.IO
        ) {
            val liveFile = getDatabaseLiveFile()
            if (!liveFile.exists()) {
                copyDatabaseAssetFile(getDatabaseLiveFile())
            } else if (shouldUpdateDatabaseFromAsset(HSKAppServices.appPreferences.appVersionCode.value)) {
                _updateProgress.value = 0f
                try {
                    HSKAppServices.snackbar.show(SnackbarType.INFO, Res.string.database_update_start)

                    val original = buildDatabase(createRoomDatabaseBuilderFromFile(liveFile))
                    val clone = original.clone()
                    _updateProgress.value = 33f
                    original.close()
                    if (clone == null) throw Exception("Couldn't clone existing db")

                    copyDatabaseAssetFile(getDatabaseLiveFile())
                    _updateProgress.value = 66f

                    val newDb = buildDatabase(createRoomDatabaseBuilderFromFile(liveFile))
                    replaceUserDataInDB(newDb, clone)
                    newDb.close()
                    clone.close()

                    _updateProgress.value = 100f
                    HSKAppServices.snackbar.show(SnackbarType.SUCCESS, Res.string.database_update_success)
                } catch (e: Exception) {
                    HSKAppServices.snackbar.show(SnackbarType.ERROR, Res.string.database_update_failure, listOf(e.message ?: ""))
                    Logging.logAnalyticsError(TAG, "UpdateDatabaseFromAssetFailure", e.message ?: "")
                } finally {
                    _updateProgress.value = null
                    cleanTempDatabaseFiles()
                }
            }

            return@withContext createRoomDatabaseBuilderFromFile(liveFile)
        }

        fun shouldUpdateDatabaseFromAsset(appVersion: Int): Boolean {
            if (appVersion == 0) return false // first launch, nothing to update

            val updateDbVersions = listOf(32, 37, 48, 64)

            return updateDbVersions.any { updateVersion ->
                appVersion < updateVersion && Utils.getAppVersion() >= updateVersion
            }
        }

        private suspend fun createRoomDatabaseBuilderFromAsset(): DatabaseBuilderWithPath = withContext(
            AppDispatchers.IO
        ) {
            val tempFile = FileKit.cacheDir / (TEMP_FILE_PREFIX + Utils.getRandomString(10))
            copyDatabaseAssetFile(tempFile)
            return@withContext createRoomDatabaseBuilderFromFile(tempFile)
        }

        suspend fun cleanTempDatabaseFiles() = withContext(AppDispatchers.IO) {
            val dir = FileKit.cacheDir
            val filesToDelete = dir.list().filter { file ->
                // Return true for files that match the pattern
                file.name.contains(TEMP_FILE_PREFIX)
            }

            // Delete the matching files
            filesToDelete.forEach { file ->
                try {
                    file.delete()
                    Logger.d(
                        tag = TAG,
                        messageString = "Deleted Temp DB file: ${file.absolutePath()}"
                    )
                } catch (_: Exception) {
                    Logger.d(
                        tag = TAG,
                        messageString = "Failed to delete temp DB file: ${file.absolutePath()}"
                    )
                }
            }
        }

        suspend fun replaceUserDataInDB(
            dbToUpdate: ChineseWordsDatabase,
            updateWith: ChineseWordsDatabase
        ) {
            withContext(AppDispatchers.IO) {
                Logger.d(tag = TAG, messageString = "Initiating Database Restoration: reading file")
                val importedAnnotations = updateWith.chineseWordAnnotationDAO().getAll()
                val importedListEntries = updateWith.wordListDAO().getUserListEntries()
                val importedLists = updateWith.wordListDAO().getUserLists()
                val importedWidgets = updateWith.widgetListDAO().getAllEntries()
                val importedFreq = updateWith.chineseWordFrequencyDAO().getAll()
                if (importedAnnotations.isEmpty() && importedListEntries.isEmpty()
                    && importedWidgets.isEmpty() && importedFreq.isEmpty()
                ) {
                    Logger.i(tag = TAG, messageString = "Backup is empty or incompatible, aborting")
                    throw IllegalStateException("Database is empty")
                }

                // Impoooort
                Logger.d(tag = TAG, messageString = "Starting to import Annotations to local DB")
                dbToUpdate.chineseWordAnnotationDAO().deleteAll()
                dbToUpdate.chineseWordAnnotationDAO().insertAll(importedAnnotations)

                Logger.d(tag = TAG, messageString = "Starting to import Word_List to local DB")
                dbToUpdate.wordListDAO().deleteAllUserEntries()
                dbToUpdate.wordListDAO().deleteAllUserLists()
                dbToUpdate.wordListDAO().insertAllLists(importedLists.map { it.wordList })
                dbToUpdate.wordListDAO().insertAllWords(importedListEntries)

                Logger.d(tag = TAG, messageString = "Starting to update the AnkiDeckIds on System lists")
                updateWith.wordListDAO().getSystemLists().forEach {
                    try {
                        dbToUpdate.wordListDAO().updateAnkiDeckId(it.id, it.ankiDeckId)
                    } catch (e: Exception) {
                        Logger.d(tag = TAG, messageString = "Couldn't update the AnkiDeckIds on list ${it.id}", throwable = e)
                    }
                }

                // Can't do the word lists population here, so pushing it to the App main process.

                Logger.d(tag = TAG, messageString = "Starting to import WordFrequency to local DB")
                dbToUpdate.chineseWordFrequencyDAO().deleteAll()
                dbToUpdate.chineseWordFrequencyDAO().insertAll(importedFreq)

                Logger.d(tag = TAG, messageString = "Starting to import WidgetList to local DB")
                dbToUpdate.widgetListDAO().deleteAllWidgets()

                /*val widgetIds = FlashcardWidgetProvider().getWidgetIds()*/
                val listIds = dbToUpdate.wordListDAO().getAllLists().map { it.id }
                val finalImportedWidgets = importedWidgets.filter {
                    /*widgetIds.contains(it.widgetId) && */ listIds.contains(it.listId)
                }

                dbToUpdate.widgetListDAO().insertListsToWidget(finalImportedWidgets)

                Logger.i(tag = TAG, messageString = "Database import done")
            }
        }
    }

    suspend fun replaceLiveUserDataFromFile(updateFrom: PlatformFile) {
        // only copy to cache if not already in cache
        var finalFile = updateFrom
        if (! updateFrom.absolutePath().contains(FileKit.cacheDir.path)) {
            finalFile = FileKit.cacheDir / (TEMP_FILE_PREFIX + updateFrom.name)
            updateFrom.copyTo(finalFile)
        }

        val sourceDb = createRoomDatabaseFromFile(finalFile)
        replaceUserDataInDB(liveDatabase, sourceDb)
        finalFile.delete()
    }

    suspend fun snapshotLiveUserDataToFile(): PlatformFile? = try {
        val newDb = _db!!.clone()
        newDb!!.truncateToUserData()
        newDb.snapshotToFile()
    } catch (_: Exception) { null }
}

expect suspend fun createRoomDatabaseBuilderFromFile(file: PlatformFile) : DatabaseBuilderWithPath

expect suspend fun copyDatabaseAssetFile(file: PlatformFile)
