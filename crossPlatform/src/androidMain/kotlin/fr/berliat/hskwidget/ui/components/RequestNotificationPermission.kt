package fr.berliat.hskwidget.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collectLatest

@Composable
fun RequestNotificationPermission(trigger: SharedFlow<Unit>,
                                  onDenied: (() -> Unit)? = null, onGranted: (() -> Unit)? = null) {
    val context = LocalContext.current

    // Launcher to request permission
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                onGranted?.invoke()
                // Permission granted
                Log.d("Permission", "POST_NOTIFICATIONS granted")
            } else {
                onDenied?.invoke()
                // Permission denied
                Log.d("Permission", "POST_NOTIFICATIONS denied")
            }
        }
    )

    LaunchedEffect(Unit) {
        trigger.collectLatest {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    if (ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                } else {
                    onGranted?.invoke()
                }
            } else {
                onGranted?.invoke() // Permission auto-granted pre-Android 13
            }
        }
    }
}