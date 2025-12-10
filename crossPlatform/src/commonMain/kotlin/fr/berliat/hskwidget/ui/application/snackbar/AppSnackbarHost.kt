package fr.berliat.hskwidget.ui.application.snackbar

import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.painterResource

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

import fr.berliat.hskwidget.core.SnackbarManager
import fr.berliat.hskwidget.core.SnackbarType
import fr.berliat.hskwidget.ui.theme.snackbarStyleFor

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

    // Custom rendering with your Surface styling
    SnackbarHost(snackbarHostState) { data ->
        val style = MaterialTheme.colorScheme.snackbarStyleFor(currentMessageType.value)

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = style.containerColor,
            contentColor = style.contentColor,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
                .padding(bottom = 75.dp)
                .padding(horizontal = 25.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                style.iconLeft?.let {
                    Icon(
                        painter = painterResource(style.iconLeft),
                        contentDescription = currentMessageType.value.name,
                        tint = style.contentColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }
                Text(
                    text = data.visuals.message,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.weight(1f))
                if (data.visuals.actionLabel != null) {
                    TextButton(onClick = { data.performAction() }) {
                        Text(
                            text = data.visuals.actionLabel!!,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
