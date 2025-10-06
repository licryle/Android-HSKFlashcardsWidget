package fr.berliat.hskwidget.domain

import co.touchlab.kermit.Logger
import fr.berliat.hskwidget.core.AppDispatchers

import fr.berliat.hskwidget.data.store.ChineseWordsDatabase

import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.absolutePath
import io.github.vinceglb.filekit.cacheDir
import io.github.vinceglb.filekit.copyTo
import io.github.vinceglb.filekit.delete
import io.github.vinceglb.filekit.div
import io.github.vinceglb.filekit.filesDir
import io.github.vinceglb.filekit.list
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.path

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class DatabaseHelper private constructor() {
    val liveDatabase
        get() = _db!!

    private var _db: ChineseWordsDatabase? = null
    lateinit var DATABASE_LIVE_PATH : PlatformFile
        private set
    lateinit var DATABASE_LIVE_DIR : PlatformFile
        private set

    companion object {
        private var INSTANCE: DatabaseHelper? = null
        private val mutex = Mutex()
        const val DATABASE_FILENAME = "Mandarin_Assistant.db"
        const val DATABASE_ASSET_PATH = "databases/$DATABASE_FILENAME"
        private const val TAG = "ChineseWordsDatabase"

        fun getDatabaseLiveDir() =  FileKit.filesDir / "../databases"
        fun getDatabaseLiveFile() = getDatabaseLiveDir() / DATABASE_FILENAME

        suspend fun getInstance(): DatabaseHelper = withContext(AppDispatchers.IO) {
            INSTANCE?.let { return@withContext it }

            mutex.withLock {
                val instance = DatabaseHelper()
                instance._db = createRoomDatabaseLive()
                instance.DATABASE_LIVE_DIR = getDatabaseLiveDir()
                instance.DATABASE_LIVE_PATH = getDatabaseLiveFile()

                // safely assign singleton
                INSTANCE = INSTANCE ?: instance
            }

            return@withContext INSTANCE!!
        }
    }

    suspend fun loadExternalDatabase(dbFilePath: PlatformFile) = withContext(
        AppDispatchers.IO) {
        return@withContext createRoomDatabaseFromFile(dbFilePath)
    }

    suspend fun cleanTempDatabaseFiles() {
        val dir = getDatabaseLiveDir()
        val filesToDelete = dir.list().filter { file ->
            // Return true for files that match the pattern
            file.name.contains("Temp_HSK_DB_")
        }

        // Delete the matching files
        filesToDelete.forEach { file ->
            try {
                file.delete()
                Logger.d(tag = TAG, messageString = "Deleted Temp DB file: ${file.absolutePath()}")
            } catch(_: Exception) {
                Logger.d(tag = TAG, messageString = "Failed to delete temp DB file: ${file.absolutePath()}")
            }
        }
    }

    suspend fun replaceUserDataInDB(dbToUpdate: ChineseWordsDatabase, updateWith: ChineseWordsDatabase) {
        withContext(AppDispatchers.IO) {
            Logger.d(tag = TAG, messageString = "Initiating Database Restoration: reading file")
            val importedAnnotations = updateWith.chineseWordAnnotationDAO().getAll()
            val importedListEntries = updateWith.wordListDAO().getAllListEntries()
            val importedLists = updateWith.wordListDAO().getAllLists()
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
            dbToUpdate.wordListDAO().deleteAllEntries()
            dbToUpdate.wordListDAO().deleteAllLists()
            dbToUpdate.wordListDAO().insertAllLists(importedLists.map { it -> it.wordList })
            dbToUpdate.wordListDAO().insertAllWords(importedListEntries)

            Logger.d(tag = TAG, messageString = "Starting to import WordFrequency to local DB")
            dbToUpdate.chineseWordFrequencyDAO().deleteAll()
            dbToUpdate.chineseWordFrequencyDAO().insertAll(importedFreq)

            Logger.d(tag = TAG, messageString = "Starting to import WidgetList to local DB")
            dbToUpdate.widgetListDAO().deleteAllWidgets()
            dbToUpdate.widgetListDAO().insertListsToWidget(importedWidgets)

            Logger.i(tag = TAG, messageString = "Database import done")
        }
    }

    suspend fun replaceLiveUserDataFromFile(updateFrom: PlatformFile) {
        // only copy to cache if not already in cache
        var finalFile = updateFrom
        if (! updateFrom.absolutePath().contains(FileKit.cacheDir.path)) {
            finalFile = FileKit.cacheDir / updateFrom.name
            updateFrom.copyTo(finalFile)
        }

        val sourceDb = loadExternalDatabase(finalFile)
        replaceUserDataInDB(liveDatabase, sourceDb)
        finalFile.delete()
    }

    suspend fun replaceWordsDataInDB(updateWith: ChineseWordsDatabase)
            = withContext(AppDispatchers.IO) {
        Logger.d(tag = TAG, messageString = "Initiating Database Update: reading file")
        val importedWordsCount = updateWith.chineseWordDAO().getCount()
        if (importedWordsCount == 0) {
            Logger.i(tag = TAG, messageString = "Update file is empty or incompatible, aborting")
            throw IllegalStateException("Database is empty")
        }

        // Inserting All succeeds, but corrupts the database. Thank you Room.
        updateWith.chineseWordDAO().getAll().chunked(5000).forEach { chunk ->
            liveDatabase.chineseWordDAO().upsertAll(chunk)
        }

        Logger.i(tag = TAG, messageString = "Database update done")
    }

    suspend fun updateLiveDatabaseFromAsset(successCallback: () -> Unit, failureCallback: (e: Exception) -> Unit)
            = withContext(AppDispatchers.IO) {
        try {
            val assetDbStream = createRoomDatabaseFromAsset()
            replaceWordsDataInDB(assetDbStream)
            assetDbStream.close()
            liveDatabase.flushToDisk()

            withContext(Dispatchers.Main) {
                successCallback()
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                failureCallback(e)
            }
        } finally {
            cleanTempDatabaseFiles()
        }
    }

    suspend fun snapshotDatabase(): PlatformFile {
        val cacheFile = FileKit.cacheDir / DATABASE_FILENAME

        withContext(AppDispatchers.IO) {
            liveDatabase.flushToDisk()

            getDatabaseLiveFile().copyTo(cacheFile)
        }

        return cacheFile
    }
}

expect suspend fun createRoomDatabaseLive() : ChineseWordsDatabase
expect suspend fun createRoomDatabaseFromFile(file: PlatformFile) : ChineseWordsDatabase
expect suspend fun createRoomDatabaseFromAsset() : ChineseWordsDatabase
