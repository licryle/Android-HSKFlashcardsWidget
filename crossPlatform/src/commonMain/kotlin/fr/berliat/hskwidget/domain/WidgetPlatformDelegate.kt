package fr.berliat.hskwidget.domain

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Interface to allow shared code to interact with native platform widget systems.
 */
interface WidgetPlatformDelegate {
    /**
     * Trigger a reload of all widget timelines.
     */
    fun reloadAll()

    /**
     * Retrieve a list of current widget configuration identifiers.
     */
    fun getWidgetIds(callback: (List<Int>) -> Unit)
}

/**
 * Suspendable version of [WidgetPlatformDelegate.getWidgetIds].
 */
suspend fun WidgetPlatformDelegate.awaitWidgetIds(): List<Int> =
    suspendCancellableCoroutine { continuation ->
        getWidgetIds { ids ->
            continuation.resume(ids)
        }
    }

/**
 * Singleton registry for the [WidgetPlatformDelegate].
 * The native app should inject its implementation here during app launch.
 */
object WidgetProvider {
    var delegate: WidgetPlatformDelegate? = null

    /**
     * Trigger a reload of all widgets if a delegate is registered.
     */
    fun triggerReload() {
        delegate?.reloadAll()
    }
}
