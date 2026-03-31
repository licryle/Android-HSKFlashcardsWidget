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
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first

import okio.Path.Companion.toPath

open class PrefixedPreferencesStore internal constructor(
    private val store: DataStore<Preferences>,
    private val prefix: String,
    private val scope: CoroutineScope? = null
) {
    internal val registeredPreferences = mutableMapOf<String, PreferenceState<*, *>>()

    companion object {
        private val mutex = Mutex()
        internal val instances = mutableMapOf<Pair<DataStore<Preferences>, String>, PrefixedPreferencesStore>()

        suspend fun getInstance(
            store: DataStore<Preferences>, 
            prefix: String, 
            scope: CoroutineScope? = null
        ): PrefixedPreferencesStore {
            val key = Pair(store, prefix)
            instances[key]?.let { return it }

            return mutex.withLock {
                instances[key] ?: PrefixedPreferencesStore(store, prefix, scope).also { instance ->
                    instances[key] = instance
                }
            }
        }

        private val dataStoreMutex = Mutex()
        internal val dataStoreInstances = mutableMapOf<String, DataStore<Preferences>>()
        suspend fun getDataStore(file: String): DataStore<Preferences> {
			val fileAbsPath = FileKit.filesDir.resolve(file).absolutePath()
				.removePrefix("file://")
                .removePrefix("file:/")
                .removePrefix("file:")
				.removePrefix("/")
				.removePrefix("//")

            dataStoreInstances[fileAbsPath]?.let { return it }

            return dataStoreMutex.withLock {
                dataStoreInstances[fileAbsPath] ?: PreferenceDataStoreFactory.createWithPath(
                    produceFile = { "/$fileAbsPath".toPath() }
                ).also { instance ->
                    dataStoreInstances[fileAbsPath] = instance
                }
            }
        }

        /**
         * Copies all preferences from one DataStore to another.
         * Useful for full migrations when keys are unknown or dynamic (like widgets).
         */
        suspend fun copyAll(source: DataStore<Preferences>, target: DataStore<Preferences>) {
            val sourceData = source.data.first()
            target.edit { prefs ->
                sourceData.asMap().forEach { (key, value) ->
                    @Suppress("UNCHECKED_CAST")
                    prefs[key as Preferences.Key<Any>] = value
                }
            }
        }
    }

    /**
     * Clears all preferences registered in this store.
     * 
     * Note: This only clears keys that have been explicitly registered via the `register*` methods.
     * This prevents accidental deletion of keys belonging to other stores with overlapping 
     * prefixes (e.g., a "user" store won't clear "user_profile" keys).
     */
    suspend fun clear() {
        store.edit { prefs ->
            registeredPreferences.values.forEach { state ->
                prefs.remove(state.key)
            }
        }
    }

    /**
     * Overwrites the values of this store with the values from another store.
     * 
     * It maps registered preferences by their short names (without prefix).
     */
    suspend fun overwriteWith(other: PrefixedPreferencesStore) {
        // Ensure both are loaded
        this.ensureAllLoaded()
        other.ensureAllLoaded()

        registeredPreferences.forEach { (keyName, state) ->
            // Extract short name by removing our prefix
            val shortName = if (prefix.isEmpty()) keyName else keyName.removePrefix("${prefix}_")
            
            // Find corresponding state in other store
            val otherKeyName = other.prefixKey(shortName)
            val otherState = other.registeredPreferences[otherKeyName]

            if (otherState != null) {
                // We use dynamic dispatch via a helper in PreferenceState to bypass type safety 
                // since we're dealing with generic PreferenceState<*, *>
                state.overwriteWith(otherState)
            }
        }
    }

    fun prefixKey(key: String): String {
        return if (prefix.isEmpty()) key else "${prefix}_$key"
    }

    /**
     * Waits for all registered preferences to be loaded from the DataStore.
     */
    suspend fun ensureAllLoaded() = coroutineScope {
        registeredPreferences.values.map { async { it.ensureLoaded() } }.awaitAll()
    }

    @Suppress("UNCHECKED_CAST")
    fun <S, T> registerPreference(
        factory: (String) -> Preferences.Key<S>,
        name: String,
        default: T,
        converter: PreferenceConverter<S, T>? = null
    ): PreferenceState<S, T> {
        val keyName = prefixKey(name)
        return (registeredPreferences.getOrPut(keyName) {
            PreferenceState(store, factory(keyName), default, converter, scope)
        } as PreferenceState<S, T>)
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
