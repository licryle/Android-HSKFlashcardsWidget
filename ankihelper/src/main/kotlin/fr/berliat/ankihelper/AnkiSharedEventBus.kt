package fr.berliat.ankihelper

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
        data class TriggerAnkiSync(val action: suspend () -> Result<Unit>) : UiEvent()
        data class AnkiServiceStarting(val serviceDelegate: AnkiSyncServiceDelegate) : UiEvent()
        data class AnkiSyncProgress(val state: AnkiSyncService.OperationState.Running) : UiEvent()
        data class AnkiSyncCancelled(val state: AnkiSyncService.OperationState.Cancelled) : UiEvent()
        data class AnkiSyncCompleted(val state: AnkiSyncService.OperationState.Completed) : UiEvent()
        data class AnkiSyncError(val state: AnkiSyncService.OperationState.Error) : UiEvent()
    }

    suspend fun emit(event: UiEvent) {
        _uiEvents.emit(event)
    }
}