package fr.berliat.hskwidget.ui.components

import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.SharedFlow

@Composable
expect fun RequestNotificationPermission(
    trigger: SharedFlow<Unit>,
    onDenied: (() -> Unit)? = null,
    onGranted: (() -> Unit)? = null
)
