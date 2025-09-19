package fr.berliat.hskwidget

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import fr.berliat.hskwidget.data.dao.AnkiDAO
import fr.berliat.hskwidget.data.store.AppPreferencesStore
import fr.berliat.hskwidget.data.store.ChineseWordsDatabase

expect object Utils {
    fun openLink(url: String)
    fun sendEmail(email: String, subject: String = "", body: String = "") : Boolean
    fun getPlatform(): String
    // commonMain
    fun getAppVersion(): String

    fun logAnalyticsScreenView(screen: String)
    fun logAnalyticsEvent(event: ANALYTICS_EVENTS)

    suspend fun getDatabaseInstance() : ChineseWordsDatabase

    fun getDataStore(file: String): DataStore<Preferences>

    fun getAnkiDAO(): AnkiDAO

    fun getDatabasePath(): String
}

fun String.capitalize() =
    this.toString().lowercase().replaceFirstChar { it.uppercaseChar() }

fun getAppPreferencesStore() : AppPreferencesStore {
    return AppPreferencesStore.getInstance(Utils.getDataStore("app.preferences_pb"))
}

enum class ANALYTICS_EVENTS {
    SCREEN_VIEW,
    AUTO_WORD_CHANGE,
    ERROR, // Use logAnalyticsError for details
    WIDGET_PLAY_WORD,
    WIDGET_MANUAL_WORD_CHANGE,
    WIDGET_RECONFIGURE,
    WIDGET_CONFIG_VIEW,
    WIGDET_RESIZE,
    WIGDET_ADD, // Would be great to add
    WIDGET_EXPAND,
    WIDGET_COLLAPSE,
    WIGDET_REMOVE,
    WIDGET_OPEN_DICTIONARY,
    WIDGET_COPY_WORD,
    CONFIG_BACKUP_ON,
    CONFIG_BACKUP_OFF,
    CONFIG_BACKUP_RESTORE,
    CONFIG_BACKUPCLOUD_ON, // Reserved for future use
    CONFIG_BACKUPCLOUD_OFF, // Reserved for future use
    CONFIG_BACKUPCLOUD_RESTORE,
    CONFIG_BACKUPCLOUD_BACKUP,
    CONFIG_ANKI_SYNC_ON,
    CONFIG_ANKI_SYNC_OFF,
    ANNOTATION_SAVE,
    ANNOTATION_DELETE,
    LIST_CREATE,
    LIST_DELETE,
    LIST_MODIFY_WORD,
    LIST_RENAME,
    DICT_HSK3_ON,
    DICT_HSK3_OFF,
    DICT_ANNOTATION_ON,
    DICT_ANNOTATION_OFF,
    DICT_SEARCH,
    OCR_WORD_NOTFOUND,
    OCR_WORD_FOUND,
    PURCHASE_CLICK,
    PURCHASE_FAILED,
    PURCHASE_SUCCESS
}