package fr.berliat.hskwidget.ui.application.snackbar

import org.jetbrains.compose.resources.getString

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

import fr.berliat.hskwidget.core.SnackbarManager
import fr.berliat.hskwidget.core.SnackbarType
import fr.berliat.hskwidget.ui.components.AppSnackbar

@Composable
fun AppSnackbarHost(
    snackbarHostState: SnackbarHostState,
    snackbarManager: SnackbarManager
) {
    val currentMessageType = remember { mutableStateOf(SnackbarType.INFO) }

    // Collect snackbar messages internally
    LaunchedEffect(snackbarHostState) {
        snackbarManager.messages.collect { message ->
            // Store the message type so it can be accessed by SnackbarHost
            currentMessageType.value = message.type

            val messageText = getString(
                message.messageRes, *message.messageArgs.toTypedArray()
            )

            val actionLabelText = message.actionLabelRes?.let {
                getString(it)
            }

            val result = snackbarHostState.showSnackbar(
                message = messageText,
                actionLabel = actionLabelText,
                duration = message.duration
            )

            when (result) {
                SnackbarResult.ActionPerformed -> message.onAction?.invoke()
                SnackbarResult.Dismissed -> message.onDismiss?.invoke()
            }
        }
    }

    SnackbarHost(snackbarHostState) { data ->
        AppSnackbar(
            message = data.visuals.message,
            type = currentMessageType.value,
            actionLabel = data.visuals.actionLabel,
            onActionClick = { data.performAction() },
            modifier = Modifier.padding(bottom = 75.dp)
        )
    }
}
