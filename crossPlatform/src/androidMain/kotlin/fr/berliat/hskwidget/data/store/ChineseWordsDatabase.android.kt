package fr.berliat.hskwidget.data.store

import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import fr.berliat.hskwidget.core.ExpectedUtils
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.absolutePath
import io.github.vinceglb.filekit.cacheDir
import io.github.vinceglb.filekit.copyTo
import io.github.vinceglb.filekit.delete
import io.github.vinceglb.filekit.div
import io.github.vinceglb.filekit.list
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.parent

actual suspend fun ChineseWordsDatabase.snapshotToFile(): PlatformFile? {
    try {
        //ToDo make it safer...
        val mainFile = this.databaseFile
        val mainCachedFile = FileKit.cacheDir / mainFile.name
        val allDbFiles = mainFile.parent()!!.list().filter { it.name.startsWith(mainFile.name) }

        val cachedDbFiles = mutableListOf<PlatformFile>()
        allDbFiles.forEach {
            val newCacheFile = FileKit.cacheDir / it.name
            it.copyTo(newCacheFile)
            cachedDbFiles.add(newCacheFile)
        }

        val factory = FrameworkSQLiteOpenHelperFactory()
        val openHelper = factory.create(
            SupportSQLiteOpenHelper.Configuration.builder(ExpectedUtils.context)
                .name(mainCachedFile.absolutePath())
                .callback(object : SupportSQLiteOpenHelper.Callback(1) {
                    override fun onCreate(db: SupportSQLiteDatabase) {}
                    override fun onUpgrade(
                        db: SupportSQLiteDatabase,
                        oldVersion: Int,
                        newVersion: Int
                    ) {
                    }
                })
                .build()
        )

        openHelper.writableDatabase
            .query("PRAGMA wal_checkpoint(full)")
            .close()
        openHelper.close()

        cachedDbFiles.filter { it.name != mainCachedFile.name }.forEach {
            // Files **should not exist**
            try {
                it.delete(false)
            } catch (_: Exception) {

            }
        }

        return mainCachedFile
    } catch (_: Exception) {
        return null
    }
}