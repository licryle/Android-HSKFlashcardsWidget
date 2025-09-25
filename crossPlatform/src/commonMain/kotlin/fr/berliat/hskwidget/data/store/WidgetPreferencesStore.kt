package fr.berliat.hskwidget.data.store

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

typealias WidgetPreferencesStoreProvider = suspend (Int) -> WidgetPreferencesStore

class WidgetPreferencesStore private constructor(store: DataStore<Preferences>, val widgetId : Int):
    PrefixedPreferencesStore(store, "widget_$widgetId") {
    companion object {
        private val mutex = Mutex()
        private val instances = mutableMapOf<Pair<DataStore<Preferences>, Int>, WidgetPreferencesStore>()

        suspend fun getInstance(store: DataStore<Preferences>, widgetId: Int): WidgetPreferencesStore {
            instances[Pair(store, widgetId)]?.let { return it }

            return mutex.withLock {
                instances[Pair(store, widgetId)] ?: WidgetPreferencesStore(store, widgetId).also { instance ->
                    instances[Pair(store, widgetId)] = instance
                }
            }
        }
    }

    val currentWord = registerPreference(::stringPreferencesKey, "current_word", "")
}