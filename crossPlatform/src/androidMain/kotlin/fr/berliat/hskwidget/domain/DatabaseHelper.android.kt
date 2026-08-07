package fr.berliat.hskwidget.domain

import androidx.room.Room

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

actual suspend fun copyDatabaseAssetFile(file: PlatformFile) {
    withContext(Dispatchers.IO) {
        val assetMgr = ExpectedUtils.context.assets

        FileKit.databasesDir.createDirectories()

        assetMgr.open(DATABASE_ASSET_PATH).use { inStream ->
            FileOutputStream(File(file.absolutePath())).use { outStream ->
                inStream.copyTo(outStream)
            }
        }
    }
}