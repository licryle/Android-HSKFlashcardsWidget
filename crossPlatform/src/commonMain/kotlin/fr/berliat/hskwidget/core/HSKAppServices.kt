package fr.berliat.hskwidget.core

import fr.berliat.hskwidget.Utils
import fr.berliat.hskwidget.data.dao.AnkiDAO
import fr.berliat.hskwidget.data.repo.WordListRepository
import fr.berliat.hskwidget.data.store.AnkiStore
import fr.berliat.hskwidget.data.store.AppPreferencesStore
import fr.berliat.hskwidget.data.store.ChineseWordsDatabase
import kotlinx.coroutines.CoroutineScope

// --- Singleton instance
object HSKAppServices : AppServices() {
    override fun init(scope: CoroutineScope) {
        register("database") { Utils.getDatabaseInstance() }
        register("appPreferences") {
            AppPreferencesStore.getInstance(Utils.getDataStore("app.preferences_pb"))
        }
        register("ankiDAO") { Utils.getAnkiDAO() }
        register("ankiStore") {
            AnkiStore(
                getAnyway("ankiDAO"),
                getAnyway<ChineseWordsDatabase>("database").wordListDAO(),
                getAnyway("appPreferences"))
        }
        register("wordListRepo") {
            WordListRepository(
                getAnyway("ankiStore"),
                getAnyway<ChineseWordsDatabase>("database").wordListDAO(),
                getAnyway<ChineseWordsDatabase>("database").annotatedChineseWordDAO()
            )
        }
        register("appScope") { scope }

        super.init(scope)
    }

    val database: ChineseWordsDatabase get() = get("database")
    val appPreferences: AppPreferencesStore get() = get("appPreferences")
    val ankiDAO: AnkiDAO get() = get("ankiDAO")
    val ankiStore: AnkiStore get() = get("ankiStore")
    val wordListRepo: WordListRepository get() = get("wordListRepo")
    val appScope: CoroutineScope get() = get("appScope")
}

