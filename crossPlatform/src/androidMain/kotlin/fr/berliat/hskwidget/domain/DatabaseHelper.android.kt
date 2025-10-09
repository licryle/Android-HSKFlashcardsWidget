package fr.berliat.hskwidget.domain

import androidx.room.Room
import androidx.room.RoomDatabase

import fr.berliat.hskwidget.core.ExpectedUtils
import fr.berliat.hskwidget.data.store.ChineseWordsDatabase

import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.absolutePath

actual suspend fun createRoomDatabaseBuilderFromFile(file: PlatformFile): RoomDatabase.Builder<ChineseWordsDatabase> {
    return Room.databaseBuilder(
        ExpectedUtils.context.applicationContext,
        ChineseWordsDatabase::class.java,
        name = file.absolutePath()
    )
    // Because of the SQLDriver in KMP, can't use createFromXXX()
}