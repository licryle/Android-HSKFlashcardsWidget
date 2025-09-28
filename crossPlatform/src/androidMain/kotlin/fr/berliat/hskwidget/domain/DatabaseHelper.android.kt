package fr.berliat.hskwidget.domain

import androidx.room.Room

import fr.berliat.hskwidget.ExpectedUtils
import fr.berliat.hskwidget.Utils
import fr.berliat.hskwidget.data.store.ChineseWordsDatabase
import fr.berliat.hskwidget.domain.DatabaseHelper.Companion.DATABASE_ASSET_PATH
import fr.berliat.hskwidget.domain.DatabaseHelper.Companion.DATABASE_FILENAME
import fr.berliat.hskwidget.domain.DatabaseHelper.Companion.getDatabaseLiveDir
import fr.berliat.hskwidget.domain.DatabaseHelper.Companion.getDatabaseLiveFile

import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.div
import io.github.vinceglb.filekit.path

import kotlinx.io.RawSource
import java.io.File

actual suspend fun createRoomDatabaseLive(): ChineseWordsDatabase {
    val db = Room.databaseBuilder(
                ExpectedUtils.context.applicationContext,
                ChineseWordsDatabase::class.java,
                DATABASE_FILENAME)
        .createFromAsset(DATABASE_ASSET_PATH)
        .build()

    db._databaseFile = getDatabaseLiveFile()

    /*if (BuildConfig.DEBUG) {
    dbBuilder.setQueryCallback(
        { sqlQuery, bindArgs ->
            Logger.d(tag = TAG, messageString = "SQL Query: $sqlQuery SQL Args: $bindArgs")
        }, Executors.newSingleThreadExecutor()
    )
}*/

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

actual suspend fun createRoomDatabaseFromStream(stream: () -> RawSource): ChineseWordsDatabase {
    val filename = "Temp_HSK_DB_${Utils.getRandomString(10)}"
    TODO()

    val db = Room.databaseBuilder(
        ExpectedUtils.context.applicationContext,
        ChineseWordsDatabase::class.java,
        "Temp_HSK_DB_${Utils.getRandomString(10)}")
        .createFromInputStream({ null })
        .build()

    db._databaseFile = getDatabaseLiveDir() / filename

    return db
}