package fr.berliat.hskwidget.core

import fr.berliat.ankidroidhelper.AnkiDelegate
import fr.berliat.hsktextviews.HSKTextSegmenter
import fr.berliat.hskwidget.KAnkiDelegator
import fr.berliat.hskwidget.KAnkiServiceDelegator
import fr.berliat.hskwidget.Utils
import fr.berliat.hskwidget.data.repo.WordListRepository
import fr.berliat.hskwidget.data.store.AnkiStore
import fr.berliat.hskwidget.data.store.AppPreferencesStore
import fr.berliat.hskwidget.data.store.ChineseWordsDatabase
import fr.berliat.hskwidget.data.store.WidgetPreferencesStore
import fr.berliat.hskwidget.data.store.WidgetPreferencesStoreProvider
import fr.berliat.hskwidget.domain.DatabaseHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// --- Singleton instance
object HSKAppServices : AppServices() {
    override fun init(scope: CoroutineScope) {
        register("appScope", 0) { scope }
        register("database", 0) { DatabaseHelper.getInstance().liveDatabase }
        register("appPreferences", 0) {
            AppPreferencesStore.getInstance(Utils.getDataStore("app.preferences_pb"))
        }

        // TODO move getDataStore to PrefixedDataPreferences
        // A unique datastore is needed, otherwise it fails silently (!) to write anything.
        val widgetDataStore = Utils.getDataStore("widgets.preferences_pb")
        register("widgetsPreferencesProvider") {
            val provider : WidgetPreferencesStoreProvider = { widgetId: Int ->
                WidgetPreferencesStore.getInstance(widgetDataStore, widgetId)
            }
            provider
        }
        register("ankiStore") {
            AnkiStore(
                Utils.getAnkiDAO(),
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
        register("HSKSegmenter") {
            val segmenter = Utils.getHSKSegmenter()

            getAnyway<CoroutineScope>("appScope").launch(Dispatchers.IO) {
                segmenter.preload()
            }

            segmenter
        }

        super.init(scope)
    }

    fun registerAnkiDelegators(ankiDelegate: AnkiDelegate) {
        register("ankiDelegate") { ankiDelegate::delegateToAnki }
        register("ankiServiceDelegate") { ankiDelegate::delegateToAnkiService }

        super.init(getAnyway("appScope"))
    }

    // P0
    val database: ChineseWordsDatabase get() = get("database")
    val appScope: CoroutineScope get() = get("appScope")

    // P2
    val appPreferences: AppPreferencesStore get() = get("appPreferences")
    val widgetsPreferencesProvider: WidgetPreferencesStoreProvider get() = get("widgetsPreferencesProvider")
    val ankiStore: AnkiStore get() = get("ankiStore")

    val ankiDelegator: KAnkiDelegator get() = get("ankiDelegate")
    val ankiServiceDelegator: KAnkiServiceDelegator get() = get("ankiServiceDelegate")
    val wordListRepo: WordListRepository get() = get("wordListRepo")
    val HSKSegmenter: HSKTextSegmenter get() = get("HSKSegmenter")
}

