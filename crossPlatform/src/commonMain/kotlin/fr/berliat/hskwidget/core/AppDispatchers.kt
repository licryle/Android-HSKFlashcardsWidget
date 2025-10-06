package fr.berliat.hskwidget.core

import kotlinx.coroutines.CoroutineDispatcher

interface CoroutineDispatchers {
    val Main: CoroutineDispatcher
    val IO: CoroutineDispatcher
    val Default: CoroutineDispatcher
}

expect val AppDispatchers: CoroutineDispatchers