package fr.berliat.hskwidget.domain

import androidx.room.Room
import androidx.room.RoomDatabase

import fr.berliat.hskwidget.data.store.ChineseWordsDatabase

import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.path
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.Foundation.*

@OptIn(ExperimentalForeignApi::class)
actual suspend fun copyDatabaseAssetFile() {
    withContext(Dispatchers.Default) { // IO dispatcher isn't a direct concept on K/N, use Default for background work
        val fileManager = NSFileManager.defaultManager()

        // 1. Get the path to the pre-populated database file within the app's bundle (assets)
        val databasePathInBundle = NSBundle.mainBundle.pathForResource(
            name = DatabaseHelper.DATABASE_FILENAME.substringBefore("."),
            ofType = DatabaseHelper.DATABASE_FILENAME.substringAfter(".")
        )

        requireNotNull(databasePathInBundle) { "Database asset file not found in bundle: ${DatabaseHelper.DATABASE_FILENAME}" }

        // 2. Define the destination path (typically the 'Documents' directory for persistent storage)
        val documentsPath = NSSearchPathForDirectoriesInDomains(
            NSDocumentDirectory,
            NSUserDomainMask,
            true
        ).first() as NSString

		val destinationDir = documentsPath.stringByAppendingPathComponent("databases") // Assuming FileKit.databasesDir points to a subdirectory
        val destinationPath = NSString.create(destinationDir).stringByAppendingPathComponent(DatabaseHelper.DATABASE_FILENAME)

        // Create destination directory if it doesn't exist (simulating FileKit.databasesDir.createDirectories())
        // Note: You may need to adapt 'FileKit.databasesDir' to its actual iOS representation.
        // For simplicity, I'll assume the destination is directly in Documents/DATABASE_FILENAME unless you provide the exact FileKit logic.

        // If you need the equivalent of FileKit.databasesDir.createDirectories():
        try {
            fileManager.createDirectoryAtPath(
                path = destinationDir,
                withIntermediateDirectories = true,
                attributes = null,
                error = null // Kotlin/Native often uses `error: NSErrorPointer?` which can be cumbersome; simplified for this example
            )
        } catch (e: Exception) {
            println("Could not create directory at $destinationDir: $e")
        }

		NSLog("INFO: copyDatabaseAssetFile $destinationPath")
        // 3. Copy the file from the bundle to the destination path
        if (!fileManager.fileExistsAtPath(destinationPath)) {
            try {
                fileManager.copyItemAtPath(
                    srcPath = databasePathInBundle,
                    toPath = destinationPath,
					error = null
                )
            } catch (e: Exception) {
                // Handle copy error
                println("Could not copy database file: $e")
            }
        }
    }
}

@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
actual suspend fun createRoomDatabaseBuilderFromFile(file: PlatformFile): RoomDatabase.Builder<ChineseWordsDatabase> {
    NSLog("INFO: createRoomDatabaseBuilderFromFile $file")

    return Room.databaseBuilder(
        name = file.path
    )
    // Because of the SQLDriver in KMP, can't use createFromXXX()
}
