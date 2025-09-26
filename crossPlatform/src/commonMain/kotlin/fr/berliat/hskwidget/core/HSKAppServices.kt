package fr.berliat.hskwidget.core

import fr.berliat.hsktextviews.HSKTextSegmenter
import fr.berliat.hskwidget.Utils
import fr.berliat.hskwidget.data.dao.AnkiDAO
import fr.berliat.hskwidget.data.repo.WordListRepository
import fr.berliat.hskwidget.data.store.AnkiStore
import fr.berliat.hskwidget.data.store.AppPreferencesStore
import fr.berliat.hskwidget.data.store.ChineseWordsDatabase
import fr.berliat.hskwidget.data.store.WidgetPreferencesStore
import fr.berliat.hskwidget.data.store.WidgetPreferencesStoreProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch

// --- Singleton instance
object HSKAppServices : AppServices() {
    override fun init(scope: CoroutineScope) {
        register("database") { Utils.getDatabaseInstance() }
        register("appPreferences") {
            AppPreferencesStore.getInstance(Utils.getDataStore("app.preferences_pb"))
        }

        // A unique datastore is needed, otherwise it fails silently (!) to write anything.
        val widgetDataStore = Utils.getDataStore("widgets.preferences_pb")
        register("widgetsPreferencesProvider") {
            val provider : WidgetPreferencesStoreProvider = { widgetId: Int ->
                WidgetPreferencesStore.getInstance(widgetDataStore, widgetId)
            }
            provider
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

        register("HSKSegmenter") {
            val segmenter = Utils.getHSKSegmenter()

            scope.launch(Dispatchers.IO) { // Async within Async
                segmenter.preload()
            }

            segmenter
        }

        super.init(scope)
    }

    val database: ChineseWordsDatabase get() = get("database")
    val appPreferences: AppPreferencesStore get() = get("appPreferences")
    val widgetsPreferencesProvider: WidgetPreferencesStoreProvider get() = get("widgetsPreferencesProvider")
    val ankiDAO: AnkiDAO get() = get("ankiDAO")
    val ankiStore: AnkiStore get() = get("ankiStore")
    val wordListRepo: WordListRepository get() = get("wordListRepo")
    val appScope: CoroutineScope get() = get("appScope")
    val HSKSegmenter: HSKTextSegmenter get() = get("HSKSegmenter")
}

