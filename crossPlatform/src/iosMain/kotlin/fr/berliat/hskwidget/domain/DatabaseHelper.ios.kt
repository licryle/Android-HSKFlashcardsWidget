package fr.berliat.hskwidget.domain

import androidx.room.Room
import androidx.room.RoomDatabase

import fr.berliat.hskwidget.data.store.ChineseWordsDatabase

import io.github.vinceglb.filekit.PlatformFile
import platform.Foundation.NSBundle
import platform.Foundation.NSLog

actual suspend fun createRoomDatabaseBuilderFromFile(file: PlatformFile): RoomDatabase.Builder<ChineseWordsDatabase> {
    NSLog("INFO: createRoomDatabaseBuilderFromFile $file")
    println("test")
    
    val testFile = NSBundle.mainBundle.pathForResource("Mandarin_Assistant", "db")
    return Room.databaseBuilder(
        name = testFile!!
    )
    // Because of the SQLDriver in KMP, can't use createFromXXX()
}