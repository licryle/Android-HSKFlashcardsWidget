package fr.berliat.hskwidget.data.dao

import fr.berliat.hskwidget.data.store.ChineseWordsDatabase

actual class DatabaseManagementDAO actual constructor(val db: ChineseWordsDatabase) {
    actual suspend fun flushDatabase() {
        db.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").close()
    }
}