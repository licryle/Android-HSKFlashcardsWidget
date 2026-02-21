package fr.berliat.hskwidget.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.concurrent.Volatile

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
        @Volatile var instance: Any? = null
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
        if (isRegistered(name))
            throw Exception("Service $name Already registered")

        services[name] = FactoryEntry(priority, factory)
        _status.value = evaluateStatus()
    }

    /**
     * Register & Init blocking a service factory.
     */
    fun <T : Any> registerNow(name: String, priority: Priority = Priority.Standard, factory: () -> T) {
        if (isRegistered(name))
            throw Exception("Service $name Already registered")
        services[name] = FactoryEntry(priority, factory, factory())
        _status.value = evaluateStatus()
    }

    /**
     * Initialize all services concurrently.
     */
    open fun init(upToLevel: Priority) {
        val currStatus = _status.value

        // If we are already ready for this level, don't restart everything
        if (currStatus is Status.Ready && currStatus.upToPrio >= upToLevel && !currStatus.partially) {
            return
        }

        // Force transition to Initialized to notify observers something is happening
        _status.value = Status.Initialized

        appScope.launch(AppDispatchers.IO) {
            try {
                services.entries
                    .filter { it.value.priority <= upToLevel && !it.value.isReady() }
                    .sortedBy { it.value.priority.priority } // highest first
                    .forEach { (_, entry) ->
                        entry.instance = entry.factory()
                    }

                _status.value = evaluateStatus()
            } catch (t: Throwable) {
                _status.value = Status.Failed(t)
            }
        }
    }

    fun isRegistered(name: String): Boolean {
        return services.containsKey(name)
    }

    @Suppress("UNCHECKED_CAST")
    fun <T: Any> get(name: String): T {
        if (!isRegistered(name))
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

        val sortedLevels = levels.entries.sortedBy { it.key }
        for (entry in sortedLevels) {
            if (entry.value) {
                minReady = entry.key
            } else {
                break // Stop at the first "gap"
            }
        }

        return if (minReady == null) {
            Status.Initialized
        } else {
            Status.Ready(minReady != maxPrio, Priority(minReady))
        }
    }
}
