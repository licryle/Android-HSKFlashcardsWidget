package fr.berliat.hskwidget.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

open class AppServices {
    private val _status = MutableStateFlow<Status>(Status.NotInitialized)
    val status: StateFlow<Status> = _status.asStateFlow()

    // Internal backing map for service factories and instances
    private val services = mutableMapOf<String, FactoryEntry>()

    val appScope: CoroutineScope get() = get("appScope")

    init {
        registerNow("appScope", Priority.Highest) {
            CoroutineScope(SupervisorJob() + AppDispatchers.Main)
        }
    }

    sealed class Status {
        object NotInitialized : Status()
        object Initialized : Status()
        data class Ready(val partially: Boolean, val upToPrio: Priority) : Status()
        data class Failed(val error: Throwable) : Status()
    }

    data class FactoryEntry(
        val priority: Priority,
        val factory: suspend () -> Any,
        var instance: Any? = null
    ) { fun isReady(): Boolean = instance != null }

    open class Priority(val priority: UInt) : Comparable<Priority> {
        constructor(other: Priority) : this(other.priority)

        override fun compareTo(other: Priority): Int = priority.compareTo(other.priority)
        override fun equals(other: Any?): Boolean = other is Priority && other.priority == this.priority
        override fun hashCode(): Int = priority.hashCode()
        override fun toString(): String = "Priority($priority)"

        object Highest : Priority(UInt.MIN_VALUE)
        object Standard : Priority(2u)
        object Lowest : Priority(UInt.MAX_VALUE)
    }

    /**
     * Register a service factory.
     */
    fun <T : Any> register(name: String, priority: Priority = Priority.Standard, factory: suspend () -> T) {
        if (services.containsKey(name)) throw Exception("Service $name Already registered")

        services[name] = FactoryEntry(priority, factory)
        _status.value = evaluateStatus()
    }

    /**
     * Register & Init blocking a service factory.
     */
    fun <T : Any> registerNow(name: String, priority: Priority = Priority.Standard, factory: () -> T) {
        if (services.containsKey(name)) throw Exception("Service $name Already registered")
        services[name] = FactoryEntry(priority, factory, factory())
    }

    /**
     * Initialize all services concurrently.
     */
    open fun init(upToLevel: HSKAppServicesPriority) {
        _status.value = Status.Initialized

        appScope.launch(AppDispatchers.IO) {
            try {
                services.entries
                    .filter { it.value.priority <= upToLevel && !it.value.isReady() }
                    .sortedBy { it.value.priority.priority } // highest first
                    .forEach { (name, entry) ->
                        entry.instance = entry.factory()
                    }
                _status.value = evaluateStatus()
            } catch (t: Throwable) {
                _status.value = Status.Failed(t)
                throw t
            }
        }
    }

    fun isRegistered(name: String): Boolean {
        return services.containsKey(name)
    }

    @Suppress("UNCHECKED_CAST")
    fun <T: Any> get(name: String): T {
        if (!services.containsKey(name))
            throw IllegalArgumentException("No service registered with name $name")

        if (!services[name]!!.isReady())
            throw IllegalArgumentException("Service $name not instantiated")

        return services[name]!!.instance as T
    }

    private fun evaluateStatus(): Status {
        val levels = mutableMapOf<UInt, Boolean>()

        // Build entries per level & readiness
        services.entries.forEach {
            levels[it.value.priority.priority] = it.value.isReady() && (levels[it.value.priority.priority] ?: true)
        }

        // Extract max level and readiness level
        val maxPrio = levels.keys.maxOrNull()
        var minReady: UInt? = null
        levels.entries.sortedBy { it.key }.forEach {
            if (!it.value) return@forEach
            minReady = it.key
        }

        return if (minReady == null) {
            Status.Initialized
        } else {
            Status.Ready(minReady == maxPrio, Priority(minReady))
        }
    }
}
