package fr.berliat.hskwidget.data.store

import android.content.Context
import android.util.Log
import androidx.room.Room
import java.io.File
import java.io.FileNotFoundException
import java.io.InputStream
import java.util.UUID
import java.util.concurrent.Callable

class DatabaseHelper private constructor() {
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

        suspend fun getInstance(context: Context): DatabaseHelper {
            INSTANCE?.let { return it }

            val instance = DatabaseHelper()
            INSTANCE = instance
            instance._db = ChineseWordsDatabase.createInstance(context)
            instance.DATABASE_LIVE_DIR = getDatabaseLiveDir(context)
            instance.DATABASE_LIVE_PATH = getDatabaseLiveFile(context)

            return instance
        }

        fun loadExternalDatabase(context: Context, dbFile: File) =
            Room.databaseBuilder(
                context.applicationContext,
                ChineseWordsDatabase::class.java,
                "Temp_HSK_DB_${UUID.randomUUID()}"
            )
                .createFromFile(dbFile) // instead of fromAsset
                .build()

        fun loadExternalDatabase(context: Context, stream: Callable<InputStream>) =
            Room.databaseBuilder(
                context.applicationContext,
                ChineseWordsDatabase::class.java,
                "Temp_HSK_DB_${UUID.randomUUID()}"
            )
                .createFromInputStream(stream) // instead of fromAsset
                .build()

        fun cleanTempDatabaseFiles(context: Context) {
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
    }

    suspend fun updateDatabaseFileOnDisk(context: Context, newDatabasePath: String) {
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
}