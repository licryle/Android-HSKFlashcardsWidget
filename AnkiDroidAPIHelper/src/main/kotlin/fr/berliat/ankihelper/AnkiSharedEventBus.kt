package fr.berliat.ankidroidhelper

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

object AnkiSharedEventBus {
    private val _uiEvents = MutableSharedFlow<UiEvent>(
        replay = 0,
        extraBufferCapacity = 10,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val uiEvents: SharedFlow<UiEvent> = _uiEvents

    sealed class UiEvent {
        data class AnkiAction(val action: suspend () -> Result<Unit>) : UiEvent()
        data class AnkiServiceStarting(val serviceDelegate: AnkiSyncServiceDelegate) : UiEvent()
        data class AnkiServiceProgress(val state: AnkiSyncService.OperationState.Running) : UiEvent()
        data class AnkiServiceCancelled(val state: AnkiSyncService.OperationState.Cancelled) : UiEvent()
        data class AnkiServiceCompleted(val state: AnkiSyncService.OperationState.Completed) : UiEvent()
        data class AnkiServiceError(val state: AnkiSyncService.OperationState.Error) : UiEvent()
    }

    suspend fun emit(event: UiEvent) {
        _uiEvents.emit(event)
    }
}