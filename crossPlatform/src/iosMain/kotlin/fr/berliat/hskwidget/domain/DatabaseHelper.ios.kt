package fr.berliat.hskwidget.domain

import androidx.room3.Room
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.createDirectories
import io.github.vinceglb.filekit.exists
import io.github.vinceglb.filekit.parent
import io.github.vinceglb.filekit.path
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.Foundation.NSBundle
import platform.Foundation.NSFileManager
import platform.Foundation.NSLog


@OptIn(ExperimentalForeignApi::class)
actual suspend fun copyDatabaseAssetFile(file: PlatformFile, overwrite: Boolean) {
    withContext(Dispatchers.Default) { // IO dispatcher isn't a direct concept on K/N, use Default for background work
        val fileManager = NSFileManager.defaultManager()

        // 1. Get the path to the pre-populated database file within the app's bundle (assets)
        val databasePathInBundle = NSBundle.mainBundle.pathForResource(
            name = DatabaseHelper.DATABASE_FILENAME.substringBefore("."),
            ofType = DatabaseHelper.DATABASE_FILENAME.substringAfter(".")
        )

        requireNotNull(databasePathInBundle) { "Database asset file not found in bundle: ${DatabaseHelper.DATABASE_FILENAME}" }

        // Fast path: nothing to do when the destination already exists and no overwrite was requested.
        if (!overwrite && file.exists()) return@withContext

        val parent = file.parent()
            ?: throw IllegalStateException("Cannot determine parent directory of ${file.path}")
        if (!parent.exists()) {
            try {
                parent.createDirectories()
            } catch (e: Exception) {
                if (!parent.exists()) {
                    throw IllegalStateException("Could not create directory for ${file.path}: $e", e)
                }
            }
        }

        NSLog("INFO: copyDatabaseAssetFile ${file.path} (overwrite=$overwrite)")
        if (overwrite && file.exists()) {
            // Remove the previous live file so the bundled asset fully replaces it,
            // including any WAL/SHM sidecars that would otherwise shadow the new file.
            fileManager.removeItemAtPath(file.path, error = null)
            fileManager.removeItemAtPath(file.path + "-wal", error = null)
            fileManager.removeItemAtPath(file.path + "-shm", error = null)
        }
        val copied = fileManager.copyItemAtPath(
            srcPath = databasePathInBundle,
            toPath = file.path,
            error = null
        )
        if (!copied || !file.exists()) {
            throw IllegalStateException("Could not copy database asset to ${file.path}")
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
actual suspend fun createRoomDatabaseBuilderFromFile(file: PlatformFile): DatabaseBuilderWithPath {
    NSLog("INFO: createRoomDatabaseBuilderFromFile $file")

    return DatabaseBuilderWithPath(
        file,
        Room.databaseBuilder(name = file.path)
    )
    // Because of the SQLDriver in KMP, can't use createFromXXX()
}
