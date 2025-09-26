package fr.berliat.hskwidget.data.store

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class PreferenceConverter<S, T>(
    val fromStore: (S) -> T,
    val toStore: (T) -> S
)

/**
 * Two-way bound preference state: updates to `.value` automatically persist to DataStore
 */
class PreferenceState<S, T>(
    private val store: DataStore<Preferences>,
    private val key: Preferences.Key<S>,
    initialValue: T,
    converter: PreferenceConverter<S, T>? = null
) {
    private val conv = converter ?: PreferenceConverter({ it as T }, { it as S })
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _flow : MutableStateFlow<T> = MutableStateFlow(initialValue)
    fun asStateFlow(): StateFlow<T> = _flow

    init {
        scope.launch {
            store.data
                .map { prefs -> prefs[key] ?: conv.toStore(initialValue) }
                .map { conv.fromStore(it) }
                .distinctUntilChanged() // only emit when actually changed
                .collect { _flow.value = it }
        }
    }

    var value: T
        get() = _flow.value
        set(v) {
            if (_flow.value != v) {         // prevent unnecessary writes
                // Save asynchronously
                scope.launch {
                    store.edit { it[key] = conv.toStore(v) }
                }
            }
        }
}