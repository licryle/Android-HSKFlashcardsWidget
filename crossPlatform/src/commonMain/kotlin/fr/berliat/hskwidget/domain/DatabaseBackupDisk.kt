package fr.berliat.hskwidget.domain

import fr.berliat.hskwidget.YYMMDDHHMMSS
import fr.berliat.hskwidget.core.HSKAppServices
import fr.berliat.hskwidget.data.store.AppPreferencesStore
import io.github.vinceglb.filekit.BookmarkData
import io.github.vinceglb.filekit.FileKit

import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.atomicMove
import io.github.vinceglb.filekit.delete
import io.github.vinceglb.filekit.dialogs.openDirectoryPicker
import io.github.vinceglb.filekit.dialogs.openFilePicker
import io.github.vinceglb.filekit.div
import io.github.vinceglb.filekit.exists
import io.github.vinceglb.filekit.fromBookmarkData
import io.github.vinceglb.filekit.isDirectory
import io.github.vinceglb.filekit.list
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.nameWithoutExtension

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import okio.FileNotFoundException

class DatabaseDiskBackup(
    private val appPreferences: AppPreferencesStore = HSKAppServices.appPreferences
) {

    /**
     * Clean old backups, keeping only [maxBackups] most recent files.
     */
    suspend fun cleanOldBackups(destinationFolder: PlatformFile, maxBackups: Int) {
        if (! destinationFolder.isDirectory()) throw IllegalStateException("Not a folder")

        withContext(Dispatchers.Default) {
            val files = destinationFolder.list()
                .filter { it.name.endsWith(DatabaseHelper.DATABASE_FILENAME) }
                .sortedByDescending { it.nameWithoutExtension } // Todo: use timestamp someday

            if (files.size > maxBackups) {
                files.drop(maxBackups).forEach { try { it.delete() } catch (_: Exception) {} }
            }
        }
    }

    fun isDirectoryAccessible(dir: PlatformFile?): Boolean {
        try {
            return dir != null
                    && dir.exists()
        } catch (_: Exception) { // Security exception on Android
            return false
        }
    }

    /**
     * Perform backup to [destinationFolder].
     */
    suspend fun backUp(
        destinationFolder: PlatformFile,
        onSuccess: () -> Unit,
        onFail: (Throwable) -> Unit
    ) {
        try {
            withContext(Dispatchers.Default) {
                val snapshot = DatabaseHelper.getInstance().snapshotDatabase()
                val timestamp = Clock.System.now()
                val fileName = "${timestamp.YYMMDDHHMMSS()}_${DatabaseHelper.DATABASE_FILENAME}"

                val destFile = destinationFolder / fileName

                snapshot.atomicMove(destFile)
            }
            onSuccess()
        } catch (e: Throwable) {
            onFail(e)
        }
    }

    /**
     * Retrieve previously set folder.
     */
    suspend fun getFolder(
        onSuccess: (PlatformFile) -> Unit,
        onFail: () -> Unit
    ) {
        val backUpFolderBookmark = appPreferences.dbBackUpDiskDirectory.value
        if (getPlatformFileFromBookmarkOrNull(backUpFolderBookmark) != null)
            onSuccess
        else {
            selectFolder(onSuccess, onFail)
        }
    }

    fun getPlatformFileFromBookmarkOrNull(bookmark: BookmarkData?): PlatformFile? {
        if (bookmark == null) return null

        val file = PlatformFile.fromBookmarkData(bookmark)
        return if (isDirectoryAccessible(file)) {
            file
        } else {
            null
        }
    }

    /**
     * Select a folder.
     */
    suspend fun selectFolder(
        onSuccess: (PlatformFile) -> Unit,
        onFail: () -> Unit
    ) {
        val dir = FileKit.openDirectoryPicker()
        if (dir == null) {
            onFail()
        } else {
            onSuccess(dir)
        }
    }

    /**
     * Select a backup file.
     */
    suspend fun selectBackupFile(
        onSuccess: (PlatformFile) -> Unit,
        onFail: (FileNotFoundException) -> Unit
    ) {
        val file = FileKit.openFilePicker()
        if (file == null || ! file.exists()) {
            onFail(FileNotFoundException("Didn't select file"))
        } else {
            onSuccess(file)
        }
    }
}
