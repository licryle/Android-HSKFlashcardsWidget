package fr.berliat.hskwidget.core

import fr.berliat.hsktextviews.HSKTextSegmenter
import fr.berliat.hskwidget.data.repo.WordListRepository
import fr.berliat.hskwidget.data.store.AnkiStore
import fr.berliat.hskwidget.data.store.AppPreferencesStore
import fr.berliat.hskwidget.data.store.ChineseWordsDatabase
import fr.berliat.hskwidget.data.store.GoogleDriveBackup
import fr.berliat.hskwidget.data.store.PrefixedPreferencesStore
import fr.berliat.hskwidget.data.store.WidgetPreferencesStore
import fr.berliat.hskwidget.data.store.WidgetPreferencesStoreProvider
import fr.berliat.hskwidget.domain.DatabaseHelper
import fr.berliat.hskwidget.domain.HSKAnkiDelegate
import fr.berliat.hskwidget.domain.KAnkiDelegator
import fr.berliat.hskwidget.domain.KAnkiServiceDelegator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

sealed class HSKAppServicesPriority(priority: UInt): AppServices.Priority(priority) {
    constructor(prio : AppServices.Priority) : this(prio.priority)

    object Widget: HSKAppServicesPriority(Highest)
    object PartialApp: HSKAppServicesPriority(Standard)
    object FullApp: HSKAppServicesPriority(Standard.priority + 1u)
}

// --- Singleton instance
object HSKAppServices : AppServices() {
    init {
        registerMostServices()
    }

    fun registerAnkiDelegators(ankiDelegate: HSKAnkiDelegate) {
        if (isRegistered("ankiDelegate")) return
        registerNow("ankiDelegate", HSKAppServicesPriority.FullApp) { ankiDelegate }
        registerNow("ankiDelegator",HSKAppServicesPriority.FullApp) { ankiDelegate::modifyAnki }
        registerNow("ankiServiceDelegator", HSKAppServicesPriority.FullApp) { ankiDelegate::modifyAnkiViaService }
    }

    fun registerGoogleBackup(gDrive: GoogleDriveBackup) {
        if (isRegistered("gDriveBackup")) return
        registerNow("gDriveBackup", HSKAppServicesPriority.PartialApp) { gDrive }
    }

    // P0
    val database: ChineseWordsDatabase get() = get("database")

    // P2
    val appPreferences: AppPreferencesStore get() = get("appPreferences")
    val widgetsPreferencesProvider: WidgetPreferencesStoreProvider get() = get("widgetsPreferencesProvider")
    val ankiStore: AnkiStore get() = get("ankiStore")

    val ankiDelegate: HSKAnkiDelegate get() = get("ankiDelegate")
    val ankiDelegator: KAnkiDelegator get() = get("ankiDelegator")
    val ankiServiceDelegator: KAnkiServiceDelegator get() = get("ankiServiceDelegator")
    val wordListRepo: WordListRepository get() = get("wordListRepo")
    val HSKSegmenter: HSKTextSegmenter get() = get("HSKSegmenter")
    val gDriveBackup: GoogleDriveBackup get() = get("gDriveBackup")

    private fun registerMostServices() {
        // Required for Widget -- Minimal Set
        register("database", HSKAppServicesPriority.Widget) { DatabaseHelper.getInstance().liveDatabase }
        register("appPreferences", HSKAppServicesPriority.Widget) {
            AppPreferencesStore.getInstance(PrefixedPreferencesStore.getDataStore("app.preferences_pb"))
        }

        register("widgetsPreferencesProvider", HSKAppServicesPriority.Widget) {
            val provider : WidgetPreferencesStoreProvider = { widgetId: Int ->
                val widgetDataStore = PrefixedPreferencesStore.getDataStore("widgets.preferences_pb")
                WidgetPreferencesStore.getInstance(widgetDataStore, widgetId)
            }
            provider
        }

        // Required for fullApp -- but still partial set (missing Anki & GoogleDrive) Set
        register("ankiStore", HSKAppServicesPriority.PartialApp) {
            AnkiStore(
                Utils.getAnkiDAO(),
                get<ChineseWordsDatabase>("database").wordListDAO(),
                get("appPreferences"))
        }
        register("wordListRepo", HSKAppServicesPriority.PartialApp) {
            WordListRepository(
                get("ankiStore"),
                get<ChineseWordsDatabase>("database").wordListDAO(),
                get<ChineseWordsDatabase>("database").annotatedChineseWordDAO()
            )
        }
        register("HSKSegmenter", HSKAppServicesPriority.PartialApp) {
            val segmenter = Utils.getHSKSegmenter()

            get<CoroutineScope>("appScope").launch(AppDispatchers.IO) {
                segmenter.preload()
            }

            segmenter
        }
    }
}

