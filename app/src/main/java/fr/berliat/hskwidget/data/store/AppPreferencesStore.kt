package fr.berliat.hskwidget.data.store

import android.content.Context
import android.net.Uri
import fr.berliat.hskwidget.data.model.ChineseWordAnnotation.ClassLevel
import fr.berliat.hskwidget.data.model.ChineseWordAnnotation.ClassType
import androidx.core.net.toUri

class AppPreferencesStore(context: Context):
    PrefixedPreferenceDataStoreBridge(context.dataStore, "app") {
    var supportTotalSpent: Float
        get() {
            return getFloat("support_total_spent", -1f)
        }
        set(total) {
            putFloat("support_total_spent", total)
        }

    var ankiSaveNotes: Boolean
        get() {
            return getBoolean("anki_save_notes", false)
        }
        set(enabled) {
            putBoolean("anki_save_notes", enabled)
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

    var searchFilterHasAnnotation: Boolean
        get() {
            return getBoolean("search_filter_hasAnnotation", false)
        }
        set(hasAnnotation) {
            putBoolean("search_filter_hasAnnotation", hasAnnotation)
        }

    var dictionaryShowHSK3Definition: Boolean
        get() {
            return getBoolean("dictionary_show_hsk3_definition", false)
        }
        set(showHSK3) {
            putBoolean("dictionary_show_hsk3_definition", showHSK3)
        }

    var lastAnnotatedClassLevel : ClassLevel
        get() {
            val lvl = getString("class_level", "NotFromClass") ?: return ClassLevel.NotFromClass

            return ClassLevel.from(lvl)
        }
        set(classLevel) {
            putString("class_level", classLevel.toString())
        }

    var lastAnnotatedClassType : ClassType
        get() {
            val type = getString("class_type", "NotFromClass") ?: return ClassType.NotFromClass

            return ClassType.from(type)
        }
        set(classType) {
            putString("class_type", classType.toString())
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
}