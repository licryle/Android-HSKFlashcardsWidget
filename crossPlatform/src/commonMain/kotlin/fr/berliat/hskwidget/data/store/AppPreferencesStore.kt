package fr.berliat.hskwidget.data.store

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.datetime.Instant
import okio.Path
import okio.Path.Companion.toPath
import fr.berliat.hskwidget.data.type.ClassLevel
import fr.berliat.hskwidget.data.type.ClassType
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Global singleton for managing app preferences via DataStore.
 *
 * This class provides a type-safe, two-way bound interface to read and write
 * preferences, supporting both primitive and complex types with automatic
 * conversion via `PreferenceConverter`.
 *
 * Key features:
 * - **Singleton access:** Use `AppPreferencesStore.getInstance(store)` to get the global instance.
 * - **Two-way bound state:** Each preference is represented as a `PreferenceState` with `.value` for
 *   synchronous in-memory access and automatic asynchronous persistence to DataStore.
 * - **StateFlow support:** Use `.asStateFlow()` on any `PreferenceState` to observe changes in Compose
 *   or coroutines.
 * - **Type conversion:** Complex types (e.g., `Path`, `Instant`, `ClassLevel`) are handled via an
 *   optional `PreferenceConverter<S, T>` providing `fromStore` and `toStore` transformations.
 * - **Coroutine-friendly:** Reads and writes happen asynchronously without blocking the main thread.
 *
 * Example usage:
 * ```
 * val prefs = AppPreferencesStore.getInstance(store)
 *
 * // Read/write value directly
 * val isActive = prefs.dbBackUpActive.value
 * prefs.dbBackUpActive.value = true
 *
 * // Observe changes in Compose
 * val showHSK3 by prefs.dictionaryShowHSK3Definition.asStateFlow().collectAsState()
 * ```
 */

class AppPreferencesStore private constructor(store: DataStore<Preferences>) {

    companion object {
        private val mutex = Mutex()
        private var INSTANCE: AppPreferencesStore? = null

        fun getInstance(store: DataStore<Preferences>): AppPreferencesStore {
            return INSTANCE ?: runBlocking {
                mutex.withLock {
                    INSTANCE ?: AppPreferencesStore(store).also { instance ->
                        INSTANCE = instance
                    }
                }
            }
        }
    }

    // --- Boolean preferences ---
    val dbBackUpActive = PreferenceState<Boolean, Boolean>(store, booleanPreferencesKey("database_backup_active"), false)
    val ankiSaveNotes = PreferenceState<Boolean, Boolean>(store, booleanPreferencesKey("anki_save_notes"), false)
    val searchFilterHasAnnotation = PreferenceState<Boolean, Boolean>(store, booleanPreferencesKey("search_filter_hasAnnotation"), false)
    val dictionaryShowHSK3Definition = PreferenceState<Boolean, Boolean>(store, booleanPreferencesKey("dictionary_show_hsk3_definition"), false)
    val readerSeparateWords = PreferenceState<Boolean, Boolean>(store, booleanPreferencesKey("reader_separate_word"), false)
    val readerShowAllPinyins = PreferenceState<Boolean, Boolean>(store, booleanPreferencesKey("reader_show_pinyins"), false)

    // --- Int preferences ---
    val appVersionCode = PreferenceState<Int, Int>(store, intPreferencesKey("appVersionCode"), 0)
    val dbBackUpMaxLocalFiles = PreferenceState<Int, Int>(store, intPreferencesKey("database_backup_max_local_files"), 2)
    val readerTextSize = PreferenceState<Int, Int>(store, intPreferencesKey("reader_text_size"), 30)

    // --- Float preferences ---
    val supportTotalSpent = PreferenceState<Float, Float>(store, floatPreferencesKey("support_total_spent"), -1f)

    // --- Derived complex types ---
    val dbBackupCloudLastSuccess = PreferenceState<Long, Instant>(store, longPreferencesKey("database_backupcloud_lastsuccess"), 0L,
        PreferenceConverter<Long, Instant>({ Instant.fromEpochMilliseconds(it) }, { it.toEpochMilliseconds() }))
    val dbBackUpDirectory = PreferenceState<String, Path>(store, stringPreferencesKey("database_backup_directory"), "",
        PreferenceConverter<String, Path>({ it.toPath() }, { it.toString() }))
    val lastAnnotatedClassLevel = PreferenceState<String, ClassLevel>(store, stringPreferencesKey("class_level"), "NotFromClass",
        PreferenceConverter<String, ClassLevel>({ ClassLevel.from(it) }, { it.name }))
    val lastAnnotatedClassType = PreferenceState<String, ClassType>(store, stringPreferencesKey("class_type"), "NotFromClass",
        PreferenceConverter<String, ClassType>({ ClassType.from(it) }, { it.name }))
}
