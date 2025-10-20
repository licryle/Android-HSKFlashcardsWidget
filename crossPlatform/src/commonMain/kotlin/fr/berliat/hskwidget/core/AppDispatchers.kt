package fr.berliat.hskwidget.core

import kotlin.coroutines.CoroutineContext

interface CoroutineDispatchers {
    val Main: CoroutineContext
    val IO: CoroutineContext
    val Default: CoroutineContext
}

expect val AppDispatchers: CoroutineDispatchers