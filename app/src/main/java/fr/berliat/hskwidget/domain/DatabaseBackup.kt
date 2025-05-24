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
import fr.berliat.hskwidget.data.store.ChineseWordsDatabase
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

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

    suspend fun backUp(destinationFolderUri: Uri): Boolean {
        Log.d(TAG, "Initiating Database Backup")
        val sourcePath = "${context.filesDir.path}/../databases/${ChineseWordsDatabase.DATABASE_FILE}"

        val current = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
        val fileName = "${current.format(formatter)}_${ChineseWordsDatabase.DATABASE_FILE}"

        return Utils.copyFileUsingSAF(context, sourcePath, destinationFolderUri, fileName)
    }

    suspend fun restoreDbFromFile(backupFile: File) {
        Log.d(TAG, "Initiating Database Restoration: reading file")
        val importedDb = ChineseWordsDatabase.loadExternalDatabase(context, backupFile)
        val importedAnnotations = importedDb.chineseWordAnnotationDAO().getAll()
        if (importedAnnotations.isEmpty()) {
            Log.i(TAG, "Backup is empty or incompatible, aborting")
            throw IllegalStateException("Database is empty")
        }

        // Impoooort
        Log.d(TAG, "Starting to import Annotations to local DB")
        val localDb = ChineseWordsDatabase.getInstance(context)
        localDb.chineseWordAnnotationDAO().deleteAll()
        localDb.chineseWordAnnotationDAO().insertAll(importedAnnotations)

        Log.d(TAG, "Starting to import Word_List_List to local DB")
        localDb.wordListDAO().deleteAllEntries()
        localDb.wordListDAO().deleteAllLists()
        localDb.wordListDAO().insertAllLists(
            importedDb.wordListDAO().getAllLists().map { it -> it.wordList })
        localDb.wordListDAO().insertAllWords(importedDb.wordListDAO().getAllEntries())

        Log.d(TAG, "Starting to import WordFrequency to local DB")
        localDb.chineseWordFrequencyDAO().deleteAll()
        localDb.chineseWordFrequencyDAO().insertAll(importedDb.chineseWordFrequencyDAO().getAll())
        Log.i(TAG, "Database import done")
    }

    companion object {
        const val TAG = "DatabaseBackup"
    }
}