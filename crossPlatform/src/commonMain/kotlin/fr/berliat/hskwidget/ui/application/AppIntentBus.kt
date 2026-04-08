package fr.berliat.hskwidget.ui.application

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlin.native.ObjCName
import kotlin.experimental.ExperimentalObjCName

@OptIn(ExperimentalObjCName::class)
@ObjCName("AppIntentBus", exact = true)
object AppIntentBus {
    private val _intents = MutableSharedFlow<AppIntent>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val intents = _intents.asSharedFlow()

    fun emit(intent: AppIntent) {
        _intents.tryEmit(intent)
    }
}