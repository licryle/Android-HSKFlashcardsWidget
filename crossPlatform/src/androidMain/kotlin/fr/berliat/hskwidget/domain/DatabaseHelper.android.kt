package fr.berliat.hskwidget.domain

import androidx.room3.Room
import fr.berliat.hskwidget.core.ExpectedUtils
import fr.berliat.hskwidget.data.store.ChineseWordsDatabase
import fr.berliat.hskwidget.domain.DatabaseHelper.Companion.DATABASE_ASSET_PATH
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.absolutePath
import io.github.vinceglb.filekit.createDirectories
import io.github.vinceglb.filekit.databasesDir
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

actual suspend fun createRoomDatabaseBuilderFromFile(file: PlatformFile): DatabaseBuilderWithPath {
    return DatabaseBuilderWithPath(
        file,
        Room.databaseBuilder(
            ExpectedUtils.context.applicationContext,
            ChineseWordsDatabase::class.java,
            name = file.absolutePath()
        )
    )
    // Because of the SQLDriver in KMP, can't use createFromXXX()
}

actual suspend fun copyDatabaseAssetFile(file: PlatformFile, overwrite: Boolean) {
    withContext(Dispatchers.IO) {
        val assetMgr = ExpectedUtils.context.assets
        val dest = File(file.absolutePath())
        if (!overwrite && dest.exists()) return@withContext

        FileKit.databasesDir.createDirectories()
        dest.parentFile?.mkdirs()

        assetMgr.open(DATABASE_ASSET_PATH).use { inStream ->
            FileOutputStream(dest).use { outStream ->
                inStream.copyTo(outStream)
            }
        }

        if (!dest.exists()) throw IllegalStateException(
            "Failed to copy database asset to ${file.absolutePath()}"
        )
    }
}
