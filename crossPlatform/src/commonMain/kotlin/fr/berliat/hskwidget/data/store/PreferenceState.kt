package fr.berliat.hskwidget.data.store

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import fr.berliat.hskwidget.core.AppDispatchers
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * Two-way bound preference state: updates to `.value` automatically persist to DataStore
 */
class PreferenceState<S, T>(
    private val store: DataStore<Preferences>,
    private val key: Preferences.Key<S>,
    private val initialValue: T,
    converter: PreferenceConverter<S, T>? = null
) {
    private val conv = converter ?: PreferenceConverter({ it as T }, { it as S })
    private val scope = CoroutineScope(SupervisorJob() + AppDispatchers.IO)
    private val _flow = MutableStateFlow(initialValue)
    private val isLoaded = CompletableDeferred<Unit>()
    
    // Configured to never block and always keep the latest value
    private val updateFlow = MutableSharedFlow<T>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    fun asStateFlow(): StateFlow<T> = _flow

    /**
     * Suspends until the initial value has been loaded from the DataStore.
     */
    suspend fun ensureLoaded() {
        isLoaded.await()
    }

    init {
        // Collect data from the store and update our local state
        scope.launch {
            store.data
                .map { prefs -> prefs[key]?.let { conv.fromStore(it) } ?: initialValue }
                .onEach { 
                    _flow.value = it 
                    if (!isLoaded.isCompleted) {
                        isLoaded.complete(Unit)
                    }
                }
                .distinctUntilChanged()
                .collect()
        }

        // Serialized and conflated writes using collectLatest
        scope.launch {
            updateFlow.collectLatest { v ->
                store.edit { it[key] = conv.toStore(v) }
            }
        }
    }

    var value: T
        get() = _flow.value
        set(v) {
            if (_flow.value != v) {
                // Optimistic update ensures immediate feedback and consistency
                _flow.value = v
                // Launch to ensure we don't block the caller, although tryEmit with DROP_OLDEST shouldn't block
                scope.launch {
                    updateFlow.emit(v)
                }
            }
        }
}
