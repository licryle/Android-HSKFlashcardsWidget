package fr.berliat.hskwidget.domain

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
