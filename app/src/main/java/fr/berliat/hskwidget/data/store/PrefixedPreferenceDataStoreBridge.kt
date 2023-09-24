package fr.berliat.hskwidget.data.store

/*** Thanks to Pittvandewitt for his PreferenceDataStoreBridge **/

import androidx.preference.PreferenceDataStore
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

open class PrefixedPreferenceDataStoreBridge(private val dataStore: DataStore<Preferences>, private val prefix: String) :
    PreferenceDataStore(), CoroutineScope by CoroutineScope(Dispatchers.IO + SupervisorJob()) {

    fun getPrefix(): String { return prefixKey("") }

    fun prefixKey(key: String): String { return prefix + '_' + key }

    override fun putString(key: String, value: String?) {
        putPreference(stringPreferencesKey(prefixKey(key)), value)
    }

    override fun putStringSet(key: String, values: MutableSet<String>?) {
        putPreference(stringSetPreferencesKey(prefixKey(key)), values)
    }

    override fun putInt(key: String, value: Int) {
        putPreference(intPreferencesKey(prefixKey(key)), value)
    }

    override fun putLong(key: String, value: Long) {
        putPreference(longPreferencesKey(prefixKey(key)), value)
    }

    override fun putFloat(key: String, value: Float) {
        putPreference(floatPreferencesKey(prefixKey(key)), value)
    }

    override fun putBoolean(key: String, value: Boolean) {
        putPreference(booleanPreferencesKey(prefixKey(key)), value)
    }

    override fun getString(key: String, defValue: String?) = runBlocking {
        dataStore.data.map { it[stringPreferencesKey(prefixKey(key))] ?: defValue }.first()
    }

    override fun getStringSet(key: String, defValues: MutableSet<String>?) = runBlocking {
        dataStore.data.map { it[stringSetPreferencesKey(prefixKey(key))] ?: defValues }.first()
    }

    override fun getInt(key: String, defValue: Int) = runBlocking {
        dataStore.data.map { it[intPreferencesKey(prefixKey(key))] ?: defValue }.first()
    }

    override fun getLong(key: String, defValue: Long) = runBlocking {
        dataStore.data.map { it[longPreferencesKey(prefixKey(key))] ?: defValue }.first()
    }

    override fun getFloat(key: String, defValue: Float) = runBlocking {
        dataStore.data.map { it[floatPreferencesKey(prefixKey(key))] ?: defValue }.first()
    }

    override fun getBoolean(key: String, defValue: Boolean) = runBlocking {
        dataStore.data.map { it[booleanPreferencesKey(prefixKey(key))] ?: defValue }.first()
    }

    private fun <T> putPreference(key: Preferences.Key<T>, value: T?) {
        launch {
            dataStore.edit {
                if (value == null) {
                    it.remove(key)
                } else {
                    it[key] = value
                }
            }
        }
    }

    fun clear() {
        GlobalScope.async {
            dataStore.edit {
                it.asMap().forEach {
                        entry ->
                    if (entry.key.name.startsWith(getPrefix()))
                        it.remove(entry.key)
                }
            }
        }
    }

    suspend fun getAllKeys(strip_prefix: Boolean) : Array<String> {
        val prefs = dataStore.data.map {
                preferences ->
            preferences.asMap().filter {
                it.key.name.startsWith(getPrefix())
            }.map {
                if (strip_prefix) {
                    it.key.name.substring(getPrefix().length)
                } else {
                    it.key.name
                }
            }
        }.first()

        return prefs.toTypedArray()
    }
}