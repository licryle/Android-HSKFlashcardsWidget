package fr.berliat.hskwidget.domain

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.provider.DocumentsContract
import android.util.Log

import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toFile

import fr.berliat.hskwidget.core.FileUtils
import fr.berliat.hskwidget.core.HSKAppServices

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import okio.Path
import okio.Path.Companion.toPath

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

actual class DatabaseDiskBackup(comp: ActivityResultCaller,
                                private val context: Context,
                                private val listener: DatabaseBackupCallbacks) {
    private val prefStore = HSKAppServices.appPreferences

    private val getFolderAct =
        comp.registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            if (uri == null || !DocumentsContract.isTreeUri(uri)) {
                listener.onBackupFolderError()
            } else {
                // Save the URI for future access
                prefStore.dbBackUpDiskDirectory.value = uri.toFile().toString().toPath()

                // Persist access permissions across reboots
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )

                // Invoke the callback with the selected URI
                listener.onBackupFolderSet(uri.toString().toPath())
            }
        }

    private val getBackUpFileAct =
        comp.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let { uri ->
                    listener.onBackupFileSelected(uri.toString().toPath())
                }
            } else {
                listener.onBackupFileSelectionCancelled()
            }
        }

    actual fun selectFolder() {
        getFolderAct.launch(null)
    }

    actual fun selectBackupFile() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
        }

        getBackUpFileAct.launch(intent)
    }

    actual fun getFolder() {
        val backUpFolderUri = prefStore.dbBackUpDiskDirectory.value
        if (FileUtils.hasFolderWritePermission(context, FileUtils.pathToUri(backUpFolderUri)))
            listener.onBackupFolderSet(backUpFolderUri)
        else {
            selectFolder()
        }
    }

    actual suspend fun cleanOldBackups(destinationFolder: Path, maxBackups: Int) =
        withContext(Dispatchers.IO) {
            val documents = FileUtils.listFilesInSAFDirectory(context, FileUtils.pathToUri(destinationFolder))
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

    actual suspend fun backUp(destinationFolder: Path): Boolean = withContext(Dispatchers.IO) {
        Log.d(TAG, "Initiating Database Backup")
        val db = DatabaseHelper.getInstance()

        val current = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
        val fileName = "${current.format(formatter)}_${DatabaseHelper.DATABASE_FILENAME}"
        val snapshotFile = db.snapshotDatabase().toFile()

        val success =
            FileUtils.copyFileUsingSAF(
                context,
                snapshotFile,
                FileUtils.pathToUri(destinationFolder),
                fileName
            )

        snapshotFile.delete()

        return@withContext success
    }

    companion object {
        const val TAG = "DatabaseBackup"
    }
}