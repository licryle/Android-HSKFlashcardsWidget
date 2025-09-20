package fr.berliat.hskwidget.data.store

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.Deferred
import java.time.Instant

class OldAppPreferencesStore(context: Context):
    PrefixedPreferenceDataStoreBridge(context.dataStore, "app") {
    var supportTotalSpent: Float
        get() {
            return getFloat("support_total_spent", -1f)
        }
        set(total) {
            putFloat("support_total_spent", total)
        }

    var appVersionCode: Int
        get() {
            return getInt("appVersionCode", 0)
        }
        set(version) {
            putInt("appVersionCode", version)
        }

    var ankiSaveNotes: Boolean
        get() {
            return getBoolean("anki_save_notes", false)
        }
        set(enabled) {
            putBoolean("anki_save_notes", enabled)
        }
    fun setAnkiSaveNotes(ankiSaveNotes: Boolean, callback: Callback?) : Deferred<Preferences> {
        return putBoolean("anki_save_notes", ankiSaveNotes, callback)
    }

    var dbBackupCloudLastSuccess: Instant
        get() {
            return Instant.ofEpochMilli(getLong("database_backupcloud_lastsuccess", 0))
        }
        set(time) {
            putLong("database_backupcloud_lastsuccess", time.toEpochMilli())
        }

    var dbBackUpActive: Boolean
        get() {
            return getBoolean("database_backup_active", false)
        }
        set(active) {
            putBoolean("database_backup_active", active)
        }

    var dbBackUpDirectory: Uri
        get() {
            return getString("database_backup_directory", "")!!.toUri()
        }
        set(dir) {
            putString("database_backup_directory", dir.toString())
        }

    var dbBackUpMaxLocalFiles: Int
        get() {
            return getInt("database_backup_max_local_files", 2)
        }
        set(nb) {
            putInt("database_backup_max_local_files", nb)
        }

    var readerTextSize : Int
        get() {
            return getInt("reader_text_size", 30)
        }
        set(textSize) {
            putInt("reader_text_size", textSize)
        }

    var readerSeparateWords: Boolean
        get() {
            return getBoolean("reader_separate_word", false)
        }
        set(enabled) {
            putBoolean("reader_separate_word", enabled)
        }

    var readerShowAllPinyins: Boolean
        get() {
            return getBoolean("reader_show_pinyins", false)
        }
        set(enabled) {
            putBoolean("reader_show_pinyins", enabled)
        }
}