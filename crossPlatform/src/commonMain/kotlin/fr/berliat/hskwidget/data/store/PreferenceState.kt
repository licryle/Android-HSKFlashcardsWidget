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
import kotlinx.coroutines.flow.first
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
    initialValue: S,
    converter: PreferenceConverter<S, T>? = null
) {
    private val conv = converter ?: PreferenceConverter({ it as T }, { it as S })
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _flow : MutableStateFlow<T> = MutableStateFlow(conv.fromStore(initialValue))
    fun asStateFlow(): StateFlow<T> = _flow

    init {
        scope.launch {
            val stored = store.data.map { it[key] ?: initialValue }.first()
            _flow.value = conv.fromStore(stored)
        }
    }

    var value: T
        get() = _flow.value
        set(v) {
            _flow.value = v
            // Save asynchronously
            scope.launch {
                store.edit { it[key] = conv.toStore(v) }
            }
        }
}