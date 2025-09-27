package fr.berliat.hskwidget.domain

import co.touchlab.kermit.Logger
import fr.berliat.hskwidget.data.model.ChineseWordAnnotation

import fr.berliat.hskwidget.data.store.ChineseWordsDatabase

import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.absolutePath
import io.github.vinceglb.filekit.atomicMove
import io.github.vinceglb.filekit.cacheDir
import io.github.vinceglb.filekit.copyTo
import io.github.vinceglb.filekit.delete
import io.github.vinceglb.filekit.div
import io.github.vinceglb.filekit.exists
import io.github.vinceglb.filekit.extension
import io.github.vinceglb.filekit.filesDir
import io.github.vinceglb.filekit.list
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.parent

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.io.RawSource
import kotlinx.io.files.FileNotFoundException

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
        const val TAG = "ChineseWordsDatabase"

        fun getDatabaseLiveDir() =  FileKit.filesDir / "../databases"
        fun getDatabaseLiveFile() = getDatabaseLiveDir() / DATABASE_FILENAME

        suspend fun getInstance(): DatabaseHelper = withContext(Dispatchers.IO) {
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
        Dispatchers.IO) {
        return@withContext createRoomDatabaseFromFile(dbFilePath)
    }

    suspend fun loadExternalDatabase(stream: () -> RawSource) = withContext(
        Dispatchers.IO) {
        return@withContext createRoomDatabaseFromStream(stream)
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

    suspend fun updateDatabaseFileOnDisk(newFile: PlatformFile)
            = withContext(Dispatchers.IO) {
        val liveFile = getDatabaseLiveFile()
        if (!newFile.exists()) {
            FileNotFoundException("No file found when trying to update")
        }

        liveDatabase.flushToDisk()
        liveDatabase.close()
        _db = null

        liveFile.delete()

        val baseFileName = newFile.name
        val filesToRename = newFile.parent()?.list()?.filter { file ->
            // Return true for files that match the pattern
            file.name.contains(baseFileName)
        } ?: emptyList()

        filesToRename.forEach {
            val ext = it.extension

            it.atomicMove(liveFile)
        }

        _db = createRoomDatabaseLive()
    }

    suspend fun replaceUserDataInDB(dbToUpdate: ChineseWordsDatabase, updateWith: ChineseWordsDatabase) {
        withContext(Dispatchers.IO) {
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

    suspend fun replaceWordsDataInDB(dbToUpdate: ChineseWordsDatabase, updateWith: ChineseWordsDatabase)
            = withContext(Dispatchers.IO) {
        Logger.d(tag = TAG, messageString = "Initiating Database Update: reading file")
        val importedWordsCount = updateWith.chineseWordDAO().getCount()
        if (importedWordsCount == 0) {
            Logger.i(tag = TAG, messageString = "Update file is empty or incompatible, aborting")
            throw IllegalStateException("Database is empty")
        }

        // Doing the opposite for efficacy: let's get user data into the importedDb, then copy file
        replaceUserDataInDB(dbToUpdate, updateWith)
        updateWith.flushToDisk()

        Logger.i(tag = TAG, messageString = "Database update done")
        return@withContext updateWith
    }

    suspend fun snapshotDatabase(): PlatformFile {
        val cacheFile = FileKit.cacheDir / DATABASE_FILENAME

        withContext(Dispatchers.IO) {
            liveDatabase.flushToDisk()

            getDatabaseLiveFile().copyTo(cacheFile)
        }

        return cacheFile
    }
}

expect suspend fun createRoomDatabaseLive() : ChineseWordsDatabase
expect suspend fun createRoomDatabaseFromFile(file: PlatformFile) : ChineseWordsDatabase
expect suspend fun createRoomDatabaseFromStream(stream: () -> RawSource) : ChineseWordsDatabase
