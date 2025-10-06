package fr.berliat.hskwidget.core

import fr.berliat.ankidroidhelper.AnkiDelegate
import fr.berliat.hsktextviews.HSKTextSegmenter
import fr.berliat.hskwidget.data.repo.WordListRepository
import fr.berliat.hskwidget.data.store.AnkiStore
import fr.berliat.hskwidget.data.store.AppPreferencesStore
import fr.berliat.hskwidget.data.store.ChineseWordsDatabase
import fr.berliat.hskwidget.data.store.GoogleDriveBackup
import fr.berliat.hskwidget.data.store.WidgetPreferencesStore
import fr.berliat.hskwidget.data.store.WidgetPreferencesStoreProvider
import fr.berliat.hskwidget.domain.DatabaseHelper
import kotlinx.coroutines.CoroutineScope
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

            getAnyway<CoroutineScope>("appScope").launch(AppDispatchers.IO) {
                segmenter.preload()
            }

            segmenter
        }

        super.init(scope)
    }

    fun registerAnkiDelegators(ankiDelegate: AnkiDelegate) {
        registerNow("ankiDelegate") { ankiDelegate }
        registerNow("ankiDelegator") { ankiDelegate::delegateToAnki }
        registerNow("ankiServiceDelegator") { ankiDelegate::delegateToAnkiService }
    }

    fun registerGoogleBackup(gDrive: GoogleDriveBackup) {
        registerNow("gDriveBackup") { gDrive }
    }

    // P0
    val database: ChineseWordsDatabase get() = get("database")
    val appScope: CoroutineScope get() = get("appScope")

    // P2
    val appPreferences: AppPreferencesStore get() = get("appPreferences")
    val widgetsPreferencesProvider: WidgetPreferencesStoreProvider get() = get("widgetsPreferencesProvider")
    val ankiStore: AnkiStore get() = get("ankiStore")

    val ankiDelegate: AnkiDelegate get() = get("ankiDelegate")
    val ankiDelegator: KAnkiDelegator get() = get("ankiDelegator")
    val ankiServiceDelegator: KAnkiServiceDelegator get() = get("ankiServiceDelegator")
    val wordListRepo: WordListRepository get() = get("wordListRepo")
    val HSKSegmenter: HSKTextSegmenter get() = get("HSKSegmenter")
    val gDriveBackup: GoogleDriveBackup get() = get("gDriveBackup")
}

