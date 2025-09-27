package fr.berliat.hskwidget.domain

import androidx.room.Room

import fr.berliat.hskwidget.ExpectedUtils
import fr.berliat.hskwidget.Utils
import fr.berliat.hskwidget.data.store.ChineseWordsDatabase
import fr.berliat.hskwidget.domain.DatabaseHelper.Companion.DATABASE_ASSET_PATH
import fr.berliat.hskwidget.domain.DatabaseHelper.Companion.DATABASE_FILENAME

import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.path

import kotlinx.io.RawSource
import java.io.File

actual suspend fun createRoomDatabaseLive(): ChineseWordsDatabase {
    return Room.databaseBuilder(
                ExpectedUtils.context().applicationContext,
                ChineseWordsDatabase::class.java,
                DATABASE_FILENAME
            )
        .createFromAsset(DATABASE_ASSET_PATH)
        .build()

            /*if (BuildConfig.DEBUG) {
            dbBuilder.setQueryCallback(
                { sqlQuery, bindArgs ->
                    Logger.d(tag = TAG, messageString = "SQL Query: $sqlQuery SQL Args: $bindArgs")
                }, Executors.newSingleThreadExecutor()
            )
        }*/
}

actual suspend fun createRoomDatabaseFromFile(file: PlatformFile): ChineseWordsDatabase {
    return Room.databaseBuilder(
        ExpectedUtils.context().applicationContext,
        ChineseWordsDatabase::class.java,
        "Temp_HSK_DB_${Utils.getRandomString(10)}")
        .createFromFile(File(file.path))
        .build()
}

actual suspend fun createRoomDatabaseFromStream(stream: () -> RawSource): ChineseWordsDatabase {
    TODO()

    return Room.databaseBuilder(
        ExpectedUtils.context().applicationContext,
        ChineseWordsDatabase::class.java,
        "Temp_HSK_DB_${Utils.getRandomString(10)}")
        .createFromInputStream({ null })
        .build()
}