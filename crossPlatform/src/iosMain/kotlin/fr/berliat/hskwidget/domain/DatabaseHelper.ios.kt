package fr.berliat.hskwidget.domain

import androidx.room.Room

import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.createDirectories
import io.github.vinceglb.filekit.exists
import io.github.vinceglb.filekit.parent
import io.github.vinceglb.filekit.path
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.Foundation.*

@OptIn(ExperimentalForeignApi::class)
actual suspend fun copyDatabaseAssetFile(file: PlatformFile) {
    withContext(Dispatchers.Default) { // IO dispatcher isn't a direct concept on K/N, use Default for background work
        val fileManager = NSFileManager.defaultManager()

        // 1. Get the path to the pre-populated database file within the app's bundle (assets)
        val databasePathInBundle = NSBundle.mainBundle.pathForResource(
            name = DatabaseHelper.DATABASE_FILENAME.substringBefore("."),
            ofType = DatabaseHelper.DATABASE_FILENAME.substringAfter(".")
        )

        requireNotNull(databasePathInBundle) { "Database asset file not found in bundle: ${DatabaseHelper.DATABASE_FILENAME}" }

        try {
            file.parent()!!.createDirectories(true)
        } catch (e: Exception) {
            println("Could not create directory for ${file.path}: $e")
        }

		NSLog("INFO: copyDatabaseAssetFile ${file.path}")
        // 3. Copy the file from the bundle to the destination path
        if (!file.exists()) {
            try {
                fileManager.copyItemAtPath(
                    srcPath = databasePathInBundle,
                    toPath = file.path,
					error = null
                )
            } catch (e: Exception) {
                // Handle copy error
                println("Could not copy database file: $e")
            }
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