package fr.berliat.hskwidget.domain

import androidx.room.Room
import co.touchlab.kermit.Logger
import fr.berliat.hskwidget.BuildKonfig

import fr.berliat.hskwidget.core.ExpectedUtils
import fr.berliat.hskwidget.core.Utils
import fr.berliat.hskwidget.data.store.ChineseWordsDatabase
import fr.berliat.hskwidget.domain.DatabaseHelper.Companion.DATABASE_ASSET_PATH
import fr.berliat.hskwidget.domain.DatabaseHelper.Companion.DATABASE_FILENAME
import fr.berliat.hskwidget.domain.DatabaseHelper.Companion.getDatabaseLiveDir
import fr.berliat.hskwidget.domain.DatabaseHelper.Companion.getDatabaseLiveFile

import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.div
import io.github.vinceglb.filekit.path

import java.io.File
import java.util.concurrent.Executors

actual suspend fun createRoomDatabaseLive(): ChineseWordsDatabase {
    val dbBuilder = Room.databaseBuilder(
                ExpectedUtils.context.applicationContext,
                ChineseWordsDatabase::class.java,
                DATABASE_FILENAME)
        .createFromAsset(DATABASE_ASSET_PATH)


    if (BuildKonfig.DEBUG_MODE) {
        dbBuilder.setQueryCallback(
            { sqlQuery, bindArgs ->
                Logger.d(tag = "DatabaseHelper", messageString = "SQL Query: $sqlQuery SQL Args: $bindArgs")
            }, Executors.newSingleThreadExecutor()
        )
    }

    val db = dbBuilder.build()
    db._databaseFile = getDatabaseLiveFile()
    return db
}

actual suspend fun createRoomDatabaseFromAsset(): ChineseWordsDatabase {
    val filename = "Temp_HSK_DB_${Utils.getRandomString(10)}"
    val db = Room.databaseBuilder(
        ExpectedUtils.context.applicationContext,
        ChineseWordsDatabase::class.java,
        filename)

        .createFromAsset(DATABASE_ASSET_PATH)
        .build()

    db._databaseFile = getDatabaseLiveDir() / filename

    return db
}

actual suspend fun createRoomDatabaseFromFile(file: PlatformFile): ChineseWordsDatabase {
    val filename = "Temp_HSK_DB_${Utils.getRandomString(10)}"
    val db = Room.databaseBuilder(
        ExpectedUtils.context.applicationContext,
        ChineseWordsDatabase::class.java,
        filename)
        .createFromFile(File(file.path))
        .build()

    db._databaseFile = getDatabaseLiveDir() / filename

    return db
}