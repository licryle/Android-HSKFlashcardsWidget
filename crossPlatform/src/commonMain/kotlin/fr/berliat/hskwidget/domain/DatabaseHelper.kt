package fr.berliat.hskwidget.domain

import fr.berliat.hskwidget.data.store.ChineseWordsDatabase
import okio.Path

expect class DatabaseHelper {
    val liveDatabase: ChineseWordsDatabase

    suspend fun loadExternalDatabase(dbFilePath: Path): ChineseWordsDatabase
    suspend fun updateDatabaseFileOnDisk(newDatabasePath: Path)
    suspend fun replaceUserDataInDB(dbToUpdate: ChineseWordsDatabase, updateWith: ChineseWordsDatabase)
    suspend fun replaceWordsDataInDB(dbToUpdate: ChineseWordsDatabase, updateWith: ChineseWordsDatabase): ChineseWordsDatabase
    suspend fun snapshotDatabase(): Path
}