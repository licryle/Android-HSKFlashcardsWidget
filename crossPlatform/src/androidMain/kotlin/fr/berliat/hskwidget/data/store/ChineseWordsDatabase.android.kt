package fr.berliat.hskwidget.data.store

import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory

import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.absolutePath

import fr.berliat.hskwidget.core.ExpectedUtils

actual suspend fun ChineseWordsDatabase.checkpointWal(file: PlatformFile) {
    val factory = FrameworkSQLiteOpenHelperFactory()
    val openHelper = factory.create(
        SupportSQLiteOpenHelper.Configuration.builder(ExpectedUtils.context)
            .name(file.absolutePath())
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
}