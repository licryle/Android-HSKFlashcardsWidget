package fr.berliat.hskwidget.data.dao

import fr.berliat.hskwidget.data.store.ChineseWordsDatabase


expect class DatabaseManagementDAO(db: ChineseWordsDatabase) {
    suspend fun flushDatabase()
}