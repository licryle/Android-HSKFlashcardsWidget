package fr.berliat.hskwidget.domain

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.contract.ActivityResultContracts
import fr.berliat.hskwidget.data.store.AppPreferencesStore
import fr.berliat.hskwidget.data.store.ChineseWordsDatabase
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

interface DatabaseBackupFolderUriCallbacks {
    fun onUriPermissionGranted(uri: Uri)
    fun onUriPermissionDenied()
}

class DatabaseBackup(comp: ActivityResultCaller,
                     private val context: Context,
                     private val listener: DatabaseBackupFolderUriCallbacks) {
    private val prefStore = AppPreferencesStore(context)

    private val getFolderAct = comp.registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri == null || ! DocumentsContract.isTreeUri(uri)) {
            listener.onUriPermissionDenied()
        } else {
            // Save the URI for future access
            prefStore.dbBackUpDirectory = uri

            // Persist access permissions across reboots
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )

            // Invoke the callback with the selected URI
            listener.onUriPermissionGranted(uri)
        }
    }

    fun selectFolder() {
        getFolderAct.launch(null)
    }

    fun getFolder() {
        val backUpFolderUri = prefStore.dbBackUpDirectory
        if (Utils.hasFolderWritePermission(context, backUpFolderUri))
            listener.onUriPermissionGranted(backUpFolderUri)
        else {
            selectFolder()
        }
    }

    suspend fun backUp(destinationFolderUri: Uri): Boolean {
        val sourcePath = "${context.filesDir.path}/../databases/${ChineseWordsDatabase.DATABASE_FILE}"

        val current = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
        val fileName = "${current.format(formatter)}_${ChineseWordsDatabase.DATABASE_FILE}"

        return Utils.copyFileUsingSAF(context, sourcePath, destinationFolderUri, fileName)
    }
}