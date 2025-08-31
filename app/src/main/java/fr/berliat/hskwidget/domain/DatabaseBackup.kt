package fr.berliat.hskwidget.domain

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.contract.ActivityResultContracts
import fr.berliat.hskwidget.data.store.AppPreferencesStore
import fr.berliat.hskwidget.data.store.DatabaseHelper
import fr.berliat.hskwidget.data.store.ChineseWordsDatabase
import java.io.InputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.Callable

interface DatabaseBackupCallbacks {
    fun onBackupFolderSet(uri: Uri)
    fun onBackupFolderError()
    fun onBackupFileSelected(uri: Uri)
    fun onBackupFileSelectionCancelled()
}

class DatabaseBackup(comp: ActivityResultCaller,
                     private val context: Context,
                     private val listener: DatabaseBackupCallbacks) {
    private val prefStore = AppPreferencesStore(context)

    private val getFolderAct = comp.registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri == null || ! DocumentsContract.isTreeUri(uri)) {
            listener.onBackupFolderError()
        } else {
            // Save the URI for future access
            prefStore.dbBackUpDirectory = uri

            // Persist access permissions across reboots
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )

            // Invoke the callback with the selected URI
            listener.onBackupFolderSet(uri)
        }
    }

    private val getBackUpFileAct = comp.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                listener.onBackupFileSelected(uri)
            }
        } else {
            listener.onBackupFileSelectionCancelled()
        }
    }

    fun selectFolder() {
        getFolderAct.launch(null)
    }

    fun selectBackupFile() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
        }

        getBackUpFileAct.launch(intent)
    }

    fun getFolder() {
        val backUpFolderUri = prefStore.dbBackUpDirectory
        if (Utils.hasFolderWritePermission(context, backUpFolderUri))
            listener.onBackupFolderSet(backUpFolderUri)
        else {
            selectFolder()
        }
    }

    suspend fun cleanOldBackups(destinationFolderUri: Uri, maxBackups: Int) {
        val documents = Utils.listFilesInSAFDirectory(context, destinationFolderUri)
            .filter { it.name?.endsWith(DatabaseHelper.DATABASE_FILENAME) == true }
            .sortedByDescending { it.lastModified() }  // Most recent first

        if (documents.size > maxBackups) {
            val toDelete = documents.drop(maxBackups)
            for (doc in toDelete) {
                try {
                    Log.d(TAG, "Deleting old backup: ${doc.name}")
                    doc.delete()
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to delete backup ${doc.name}", e)
                }
            }
        }
    }

    suspend fun backUp(destinationFolderUri: Uri): Boolean {
        Log.d(TAG, "Initiating Database Backup")
        val db = DatabaseHelper.getInstance(context)
        val sourcePath = db.DATABASE_LIVE_PATH

        val current = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
        val fileName = "${current.format(formatter)}_${DatabaseHelper.DATABASE_FILENAME}"

        db.flushToDisk()

        return Utils.copyFileUsingSAF(context, sourcePath, destinationFolderUri, fileName)
    }

    suspend fun replaceUserDataInDB(dbToUpdate: ChineseWordsDatabase, updateWith: ChineseWordsDatabase) {
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

    suspend fun replaceWordsDataInDB(dbToUpdate: Callable<InputStream>, updateWith: Callable<InputStream>): ChineseWordsDatabase {
        Log.d(TAG, "Initiating Database Update: reading file")
        val importedDb = DatabaseHelper.loadExternalDatabase(context, updateWith)
        val importedWordsCount = importedDb.chineseWordDAO().getCount()
        if (importedWordsCount == 0) {
            Log.i(TAG, "Update file is empty or incompatible, aborting")
            throw IllegalStateException("Database is empty")
        }

        val userDataDb = DatabaseHelper.loadExternalDatabase(context, dbToUpdate)
        // Doing the opposite for efficacy: let's get user data into the importedDb, then copy file
        replaceUserDataInDB(importedDb, userDataDb)
        importedDb.flushToDisk()

        Log.i(TAG, "Database update done")
        return importedDb
    }

    companion object {
        const val TAG = "DatabaseBackup"
    }
}