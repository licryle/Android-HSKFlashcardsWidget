package fr.berliat.hskwidget.core

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class FactoryEntry(
    val priority: Int,
    val factory: suspend () -> Any
)

open class AppServices {
    private val _status = MutableStateFlow<Status>(Status.NotReady)
    val status: StateFlow<Status> = _status.asStateFlow()

    // Internal backing map for service factories and instances
    private val factories = mutableMapOf<String, FactoryEntry>()
    private val instances = mutableMapOf<String, Any>()
    private val mutex = Mutex()

    sealed class Status {
        object NotReady : Status()
        object Ready : Status()
        data class Failed(val error: Throwable) : Status()
    }

    /**
     * Register a service factory.
     */
    fun <T : Any> register(name: String, priority: Int = 2, factory: suspend () -> T) {
        if (factories.containsKey(name)) throw Exception("Service $name Already registered")

        factories[name] = FactoryEntry(priority, factory)
        _status.value = Status.NotReady
    }

    /**
     * Initialize all services concurrently.
     */
    open fun init(scope: CoroutineScope) {
        scope.launch(Dispatchers.IO) {
            try {
                factories.entries
                    .sortedBy { it.value.priority } // highest first
                    .forEach { (name, entry) ->
                        if (!instances.containsKey(name)) {
                            val instance = entry.factory()
                            mutex.withLock {
                                instances[name] = instance
                            }
                        }
                    }
                _status.value = Status.Ready
            } catch (t: Throwable) {
                _status.value = Status.Failed(t)
                throw t
            }
        }
    }

    fun isRegistered(name: String): Boolean {
        return instances[name] != null
    }

    fun <T: Any> getAnyway(name: String): T {
        return instances[name] as? T
            ?: throw IllegalArgumentException("No service registered with name $name")
    }

    /**
     * Safe getter for a service.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> get(name: String): T {
        if (_status.value != Status.Ready) {
            throw IllegalStateException("AppServices not ready yet!")
        }
        return instances[name] as? T
            ?: throw IllegalArgumentException("No service registered with name $name")
    }
}
