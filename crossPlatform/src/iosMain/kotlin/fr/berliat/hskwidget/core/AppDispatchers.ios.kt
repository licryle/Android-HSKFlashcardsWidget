package fr.berliat.hskwidget.core

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

actual val AppDispatchers: CoroutineDispatchers = object : CoroutineDispatchers {
    override val Main: CoroutineDispatcher = Dispatchers.Main
    override val IO: CoroutineDispatcher = Dispatchers.Default // fallback
    override val Default: CoroutineDispatcher = Dispatchers.Default
}