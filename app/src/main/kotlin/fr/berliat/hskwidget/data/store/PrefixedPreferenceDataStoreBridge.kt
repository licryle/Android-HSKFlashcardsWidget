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
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

typealias Callback = () -> Unit

open class PrefixedPreferenceDataStoreBridge(private val dataStore: DataStore<Preferences>, private val prefix: String) :
    PreferenceDataStore() {

    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private fun getPrefix(): String { return prefixKey("") }

    private fun prefixKey(key: String): String { return prefix + '_' + key }

    override fun putString(key: String, value: String?) { putString(key, value, null) }
    fun putString(key: String, value: String?, callback: Callback?) : Deferred<Preferences> {
        return putPreference(stringPreferencesKey(prefixKey(key)), value, callback)
    }

    override fun putStringSet(key: String, values: MutableSet<String>?) { putStringSet(key, values, null) }
    fun putStringSet(key: String, values: MutableSet<String>?, callback: Callback?) : Deferred<Preferences> {
        return putPreference(stringSetPreferencesKey(prefixKey(key)), values, callback)
    }

    override fun putInt(key: String, value: Int) { putInt(key, value, null) }
    fun putInt(key: String, value: Int, callback: Callback? = null) : Deferred<Preferences> {
        return putPreference(intPreferencesKey(prefixKey(key)), value, callback)
    }

    override fun putLong(key: String, value: Long) { putLong(key, value, null) }
    fun putLong(key: String, value: Long, callback: Callback?) : Deferred<Preferences> {
        return putPreference(longPreferencesKey(prefixKey(key)), value, callback)
    }

    override fun putFloat(key: String, value: Float) { putFloat(key, value, null) }
    fun putFloat(key: String, value: Float, callback: Callback?) : Deferred<Preferences> {
        return putPreference(floatPreferencesKey(prefixKey(key)), value, callback)
    }

    override fun putBoolean(key: String, value: Boolean) { putBoolean(key, value, null)}
    fun putBoolean(key: String, value: Boolean, callback: Callback?) : Deferred<Preferences> {
        return putPreference(booleanPreferencesKey(prefixKey(key)), value, callback)
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

    private fun <T> putPreference(key: Preferences.Key<T>, value: T?, callback: Callback? = null) : Deferred<Preferences> {
       return coroutineScope.async {
            val pref = dataStore.edit {
                if (value == null) {
                    it.remove(key)
                } else {
                    it[key] = value
                }
            }

            withContext(Dispatchers.Main) {
                callback?.invoke()
            }

            pref
        }
    }

    fun clear(callback: Callback? = null): Deferred<Preferences> {
        return coroutineScope.async {
            val pref = dataStore.edit {
                it.asMap().forEach {
                        entry ->
                    if (entry.key.name.startsWith(getPrefix()))
                        it.remove(entry.key)
                }
            }

            withContext(Dispatchers.Main) {
                callback?.invoke()
            }

            pref
        }
    }

    suspend fun getAllKeys(stripPrefix: Boolean) : Array<String> = withContext(Dispatchers.IO) {
        val prefs = dataStore.data.map {
                preferences ->
            preferences.asMap().filter {
                it.key.name.startsWith(getPrefix())
            }.map {
                if (stripPrefix) {
                    it.key.name.substring(getPrefix().length)
                } else {
                    it.key.name
                }
            }
        }.first()

        return@withContext prefs.toTypedArray()
    }
}