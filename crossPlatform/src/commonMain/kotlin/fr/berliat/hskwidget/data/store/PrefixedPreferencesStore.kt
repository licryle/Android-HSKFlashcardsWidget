package fr.berliat.hskwidget.data.store

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey

import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.absolutePath
import io.github.vinceglb.filekit.filesDir
import io.github.vinceglb.filekit.resolve

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

import okio.Path.Companion.toPath

open class PrefixedPreferencesStore protected constructor(
    private val store: DataStore<Preferences>,
    private val prefix: String
) {
    companion object {
        private val mutex = Mutex()
        private val instances = mutableMapOf<Pair<DataStore<Preferences>, String>, PrefixedPreferencesStore>()

        suspend fun getInstance(store: DataStore<Preferences>, prefix: String): PrefixedPreferencesStore {
            val key = Pair(store, prefix)
            instances[key]?.let { return it }

            return mutex.withLock {
                instances[key] ?: PrefixedPreferencesStore(store, prefix).also { instance ->
                    instances[key] = instance
                }
            }
        }

        private val dataStoreMutex = Mutex()
        private val dataStoreInstances = mutableMapOf<String, DataStore<Preferences>>()
        suspend fun getDataStore(file: String): DataStore<Preferences> {
			val fileAbsPath = FileKit.filesDir.resolve(file).absolutePath()
				.removePrefix("file://") // Handles the case where it starts with file://
				.removePrefix("file:")   // Handles the case where it starts with file:/ (your error)
				.removePrefix("/")       // Ensures we don't double-slash
				.removePrefix("//")      // Ensures we don't have multiple slashes at the start

            dataStoreInstances[fileAbsPath]?.let { return it }

            return dataStoreMutex.withLock {
                dataStoreInstances[fileAbsPath] ?: PreferenceDataStoreFactory.createWithPath(
                    produceFile = { "/$fileAbsPath".toPath() }
                ).also { instance ->
                    dataStoreInstances[fileAbsPath] = instance
                }
            }
        }
    }

    suspend fun clear() {
        store.edit { prefs ->
            val keysToRemove = prefs.asMap()
                .keys
                .filter { it.name.startsWith(prefix) }

            keysToRemove.forEach { prefs.remove(it) }
        }
    }

    fun prefixKey(key: String): String {
        return if (prefix.isEmpty()) key else "${prefix}_$key"
    }

    fun <S, T> registerPreference(
        factory: (String) -> Preferences.Key<S>,
        name: String,
        default: T,
        converter: PreferenceConverter<S, T>? = null
    ): PreferenceState<S, T> {
        val key = prefixKey(name)
        return PreferenceState(store, factory(key), default, converter)
    }

    fun <T> registerBooleanPref(name: String, default: T, converter: PreferenceConverter<Boolean, T>? = null) =
        registerPreference(::booleanPreferencesKey, name, default, converter)

    fun <T> registerIntPref(name: String, default: T, converter: PreferenceConverter<Int, T>? = null) =
        registerPreference(::intPreferencesKey, name, default, converter)

    fun <T> registerLongPref(name: String, default: T, converter: PreferenceConverter<Long, T>? = null) =
        registerPreference(::longPreferencesKey, name, default, converter)

    fun <T> registerFloatPref(name: String, default: T, converter: PreferenceConverter<Float, T>? = null) =
        registerPreference(::floatPreferencesKey, name, default, converter)

    fun <T> registerStringPref(name: String, default: T, converter: PreferenceConverter<String, T>? = null) =
        registerPreference(::stringPreferencesKey, name, default, converter)

    fun <T> registerStringSetPref(name: String, default: T, converter: PreferenceConverter<Set<String>, T>? = null) =
        registerPreference(::stringSetPreferencesKey, name, default, converter)
}
