package fr.berliat.hskwidget.domain

import androidx.room.Room
import androidx.room.RoomDatabase

import fr.berliat.hskwidget.core.ExpectedUtils
import fr.berliat.hskwidget.data.store.ChineseWordsDatabase
import fr.berliat.hskwidget.domain.DatabaseHelper.Companion.DATABASE_ASSET_PATH
import fr.berliat.hskwidget.domain.DatabaseHelper.Companion.DATABASE_FILENAME

import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.absolutePath
import io.github.vinceglb.filekit.createDirectories
import io.github.vinceglb.filekit.databasesDir

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import java.io.File
import java.io.FileOutputStream

actual suspend fun createRoomDatabaseBuilderFromFile(file: PlatformFile): RoomDatabase.Builder<ChineseWordsDatabase> {
    return Room.databaseBuilder(
        ExpectedUtils.context.applicationContext,
        ChineseWordsDatabase::class.java,
        name = file.absolutePath()
    )
    // Because of the SQLDriver in KMP, can't use createFromXXX()
}

actual suspend fun copyDatabaseAssetFile() {
    withContext(Dispatchers.IO) {
        val assetMgr = ExpectedUtils.context.assets

        FileKit.databasesDir.createDirectories()

        val file = File("${FileKit.databasesDir.absolutePath()}/$DATABASE_FILENAME")

        assetMgr.open(DATABASE_ASSET_PATH).use { inStream ->
            FileOutputStream(file).use { outStream ->
                inStream.copyTo(outStream)
            }
        }
    }
}