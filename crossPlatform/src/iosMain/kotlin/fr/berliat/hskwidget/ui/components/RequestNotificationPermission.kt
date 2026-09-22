package fr.berliat.hskwidget.ui.components

import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.SharedFlow

@Composable
actual fun RequestNotificationPermission(
    trigger: SharedFlow<Unit>,
    onDenied: (() -> Unit)?,
    onGranted: (() -> Unit)?
) {
    // No-op on iOS
}
