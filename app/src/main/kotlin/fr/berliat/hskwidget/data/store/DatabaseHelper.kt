package fr.berliat.hskwidget.data.store

import android.content.Context
import android.util.Log
import androidx.core.net.toUri
import androidx.room.Room
import fr.berliat.hskwidget.domain.Utils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileNotFoundException
import java.io.InputStream
import java.util.UUID
import java.util.concurrent.Callable

class DatabaseHelper private constructor(val context: Context) {
    fun annotatedChineseWordDAO() = liveDatabase.annotatedChineseWordDAO()
    fun chineseWordAnnotationDAO() = liveDatabase.chineseWordAnnotationDAO()
    fun chineseWordDAO() = liveDatabase.chineseWordDAO()
    fun chineseWordFrequencyDAO() = liveDatabase.chineseWordFrequencyDAO()
    fun wordListDAO() = liveDatabase.wordListDAO()
    fun widgetListDAO() = liveDatabase.widgetListDAO()
    fun flushToDisk() = liveDatabase.flushToDisk()
    val liveDatabase
        get() = _db!!
    val databasePath
        get() = liveDatabase.databasePath

    private var _db: ChineseWordsDatabase? = null
    var DATABASE_LIVE_PATH : String = ""
        private set
    var DATABASE_LIVE_DIR : String = ""
        private set

    companion object {
        @Volatile
        private var INSTANCE: DatabaseHelper? = null
        const val DATABASE_FILENAME = "Mandarin_Assistant.db"
        const val DATABASE_ASSET_PATH = "databases/$DATABASE_FILENAME"
        const val TAG = "ChineseWordsDatabase"

        fun getDatabaseLiveDir(context: Context) =  "${context.filesDir.path}/../databases"
        fun getDatabaseLiveFile(context: Context) = getDatabaseLiveDir(context) + "/${DATABASE_FILENAME}"

        suspend fun getInstance(context: Context): DatabaseHelper = withContext(Dispatchers.IO) {
            INSTANCE?.let { return@withContext it }

            val instance = DatabaseHelper(context)
            INSTANCE = instance
            instance._db = ChineseWordsDatabase.createInstance(context)
            instance.DATABASE_LIVE_DIR = getDatabaseLiveDir(context)
            instance.DATABASE_LIVE_PATH = getDatabaseLiveFile(context)

            return@withContext instance
        }
    }

    fun loadExternalDatabase(dbFile: File) =
        Room.databaseBuilder(
            context,
            ChineseWordsDatabase::class.java,
            "Temp_HSK_DB_${UUID.randomUUID()}"
        )
            .createFromFile(dbFile) // instead of fromAsset
            .build()

    fun loadExternalDatabase(stream: Callable<InputStream>) =
        Room.databaseBuilder(
            context,
            ChineseWordsDatabase::class.java,
            "Temp_HSK_DB_${UUID.randomUUID()}"
        )
            .createFromInputStream(stream) // instead of fromAsset
            .build()

    fun cleanTempDatabaseFiles() {
        val dir = File(getDatabaseLiveDir(context))
        val filesToDelete = dir.listFiles { file ->
            // Return true for files that match the pattern
            file.name.contains("Temp_HSK_DB_")
        }

        // Delete the matching files
        filesToDelete?.forEach { file ->
            val deleted = file.delete()
            if (deleted) {
                Log.d(TAG, "Deleted Temp DB file: ${file.absolutePath}")
            } else {
                Log.d(TAG, "Failed to delete temp DB file: ${file.absolutePath}")
            }
        }
    }

    suspend fun updateDatabaseFileOnDisk(newDatabasePath: String) = withContext(Dispatchers.IO) {
        val newFile = File(newDatabasePath)
        val oldFile = File(getDatabaseLiveFile(context))
        if (!newFile.exists()) {
            FileNotFoundException()
        }

        flushToDisk()
        liveDatabase.close()
        _db = null

        oldFile.delete()

        val baseFileName = newFile.name
        val filesToRename = newFile.parentFile?.listFiles { file ->
            // Return true for files that match the pattern
            file.name.contains(baseFileName)
        }
        filesToRename?.forEach {
            val ext = it.name.toString().substring(baseFileName.length)
            it.renameTo(File(oldFile.absoluteFile.toString() + ext))
        }

        _db = ChineseWordsDatabase.createInstance(context)
    }

    suspend fun replaceUserDataInDB(dbToUpdate: ChineseWordsDatabase, updateWith: ChineseWordsDatabase)
            = withContext(Dispatchers.IO) {
        Log.d(TAG, "Initiating Database Restoration: reading file")
        val importedAnnotations = updateWith.chineseWordAnnotationDAO().getAll()
        val importedListEntries = updateWith.wordListDAO().getAllListEntries()
        val importedLists = updateWith.wordListDAO().getAllLists()
        val importedWidgets = updateWith.widgetListDAO().getAllEntries()
        val importedFreq = updateWith.chineseWordFrequencyDAO().getAll()
        if (importedAnnotations.isEmpty() && importedListEntries.isEmpty()
            && importedWidgets.isEmpty() && importedFreq.isEmpty()) {
            Log.i(TAG, "Backup is empty or incompatible, aborting")
            throw IllegalStateException("Database is empty")
        }

        // Impoooort
        Log.d(TAG, "Starting to import Annotations to local DB")
        dbToUpdate.chineseWordAnnotationDAO().deleteAll()
        dbToUpdate.chineseWordAnnotationDAO().insertAll(importedAnnotations)

        Log.d(TAG, "Starting to import Word_List to local DB")
        dbToUpdate.wordListDAO().deleteAllEntries()
        dbToUpdate.wordListDAO().deleteAllLists()
        dbToUpdate.wordListDAO().insertAllLists(importedLists.map { it -> it.wordList })
        dbToUpdate.wordListDAO().insertAllWords(importedListEntries)

        Log.d(TAG, "Starting to import WordFrequency to local DB")
        dbToUpdate.chineseWordFrequencyDAO().deleteAll()
        dbToUpdate.chineseWordFrequencyDAO().insertAll(importedFreq)

        Log.d(TAG, "Starting to import WidgetList to local DB")
        dbToUpdate.widgetListDAO().deleteAllWidgets()
        dbToUpdate.widgetListDAO().insertListsToWidget(importedWidgets)

        Log.i(TAG, "Database import done")
    }

    suspend fun replaceWordsDataInDB(dbToUpdate: Callable<InputStream>, updateWith: Callable<InputStream>): ChineseWordsDatabase
            = withContext(Dispatchers.IO) {
        Log.d(TAG, "Initiating Database Update: reading file")
        val importedDb = loadExternalDatabase(updateWith)
        val importedWordsCount = importedDb.chineseWordDAO().getCount()
        if (importedWordsCount == 0) {
            Log.i(TAG, "Update file is empty or incompatible, aborting")
            throw IllegalStateException("Database is empty")
        }

        val userDataDb = loadExternalDatabase(dbToUpdate)
        // Doing the opposite for efficacy: let's get user data into the importedDb, then copy file
        replaceUserDataInDB(importedDb, userDataDb)
        importedDb.flushToDisk()

        Log.i(TAG, "Database update done")
        return@withContext importedDb
    }

    suspend fun snapshotDatabase(): File = withContext(Dispatchers.IO) {
        flushToDisk()

        return@withContext Utils.copyUriToCacheDir(context, File(DATABASE_LIVE_PATH).toUri())
    }
}