package fr.berliat.hskwidget.core

import fr.berliat.hskwidget.Utils
import fr.berliat.hskwidget.data.store.ChineseWordsDatabase
import fr.berliat.hskwidget.getAppPreferencesStore

// --- Singleton instance
object HSKAppServices : AppServices() {
    init {
        register("database") { Utils.getDatabaseInstance() }
        register("appPreferences") { getAppPreferencesStore() }
    }

    val database: ChineseWordsDatabase get() = get("database")
    val appPreferences: ChineseWordsDatabase get() = get("appPreferences")
}

