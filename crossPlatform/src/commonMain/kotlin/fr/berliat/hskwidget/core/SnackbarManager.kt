package fr.berliat.hskwidget.core

import androidx.compose.material3.SnackbarDuration
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.jetbrains.compose.resources.StringResource

/**
 * Data class representing a snackbar message with optional action and callbacks.
 *
 * @param messageRes The string resource for the message to display
 * @param messageArgs Arguments to format the message string resource
 * @param duration How long to show the snackbar
 * @param actionLabelRes Optional string resource for an action button label
 * @param onAction Optional callback invoked when the action is clicked
 * @param onDismiss Optional callback invoked when the snackbar is dismissed
 */
data class SnackbarMessage(
    val messageRes: StringResource,
    val messageArgs: List<String> = emptyList(),
    val duration: SnackbarDuration = SnackbarDuration.Short,
    val actionLabelRes: StringResource? = null,
    val onAction: (() -> Unit)? = null,
    val onDismiss: (() -> Unit)? = null
)

/**
 * Global message bus for showing snackbars throughout the app.
 * Uses a Flow-based pattern to decouple snackbar requests from the UI layer.
 *
 * Usage:
 * - Call [show] from anywhere in the app to queue a snackbar message
 * - Collect [messages] in your Scaffold to display snackbars
 */
object SnackbarManager {
    private val _messages = MutableSharedFlow<SnackbarMessage>(
        replay = 0,
        extraBufferCapacity = 10, // Buffer up to 10 messages
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    
    /**
     * Flow of snackbar messages. Collect this in your UI layer to display snackbars.
     */
    val messages: SharedFlow<SnackbarMessage> = _messages.asSharedFlow()
    
    /**
     * Show a snackbar with the given parameters.
     *
     * @param messageRes The string resource for the message to display
     * @param messageArgs Arguments to format the message (default: empty)
     * @param duration How long to show the snackbar (default: Short)
     * @param actionLabelRes Optional string resource for an action button label
     * @param onAction Optional callback invoked when the action is clicked
     * @param onDismiss Optional callback invoked when the snackbar is dismissed
     */
    fun show(
        messageRes: StringResource,
        messageArgs: List<String> = emptyList(),
        duration: SnackbarDuration = SnackbarDuration.Long,
        actionLabelRes: StringResource? = null,
        onAction: (() -> Unit)? = null,
        onDismiss: (() -> Unit)? = null
    ) {
        _messages.tryEmit(
            SnackbarMessage(
                messageRes = messageRes,
                messageArgs = messageArgs,
                duration = duration,
                actionLabelRes = actionLabelRes,
                onAction = onAction,
                onDismiss = onDismiss
            )
        )
    }
}
