package fr.berliat.hskwidget.data.dao

import fr.berliat.hskwidget.data.store.ChineseWordsDatabase

actual class DatabaseManagementDAO actual constructor(db: ChineseWordsDatabase) {
    actual suspend fun flushDatabase() {
        // TODO write the flush
    }
}