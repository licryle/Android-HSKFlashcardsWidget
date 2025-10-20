package fr.berliat.hskwidget.core

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.CoroutineContext

actual val AppDispatchers: CoroutineDispatchers = object : CoroutineDispatchers {
    override val Main: CoroutineContext = Dispatchers.Main + Logging.GlobalCoroutineExceptionHandler
    override val IO: CoroutineContext = Dispatchers.Default + Logging.GlobalCoroutineExceptionHandler // fallback
    override val Default: CoroutineContext = Dispatchers.Default + Logging.GlobalCoroutineExceptionHandler
}