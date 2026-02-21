package fr.berliat.hskwidget.ui.screens.OCR

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

import co.touchlab.kermit.Logger

import com.kashif.cameraK.compose.CameraKScreen
import com.kashif.cameraK.compose.rememberCameraKState
import com.kashif.cameraK.permissions.providePermissions
import com.kashif.cameraK.state.CameraConfiguration
import com.kashif.cameraK.enums.*

import fr.berliat.hskwidget.ui.components.Error
import fr.berliat.hskwidget.ui.components.ErrorView
import fr.berliat.hskwidget.ui.components.LoadingProgressView

import fr.berliat.hskwidget.Res
import fr.berliat.hskwidget.ic_launcher
import fr.berliat.hskwidget.ocr_capture_btn
import fr.berliat.hskwidget.ocr_capture_permission_denied

import io.github.vinceglb.filekit.PlatformFile

import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private const val TAG = "CaptureImageScreen"

@Composable
fun CaptureImageScreen(
    modifier: Modifier = Modifier,
    onImageReady: (PlatformFile) -> Unit,
    viewModel: CaptureImageViewModel = remember { CaptureImageViewModel(onImageReady) }
) {
    val permissions = providePermissions()
    val cameraPermissionState = remember { mutableStateOf(permissions.hasCameraPermission()) }
    val storagePermissionState = remember { mutableStateOf(permissions.hasStoragePermission()) }
    val retryPermissions = remember { mutableStateOf(true) }

    if (retryPermissions.value) {
        if (!cameraPermissionState.value) {
            permissions.RequestCameraPermission(
                onGranted = { cameraPermissionState.value = true },
                onDenied = {
                    Logger.e(tag = TAG, messageString = "Camera Permission Denied")
                }
            )
        }

        if (!storagePermissionState.value) {
            permissions.RequestStoragePermission(
                onGranted = { storagePermissionState.value = true },
                onDenied = {
                    Logger.e(tag = TAG, messageString = "Folder Permission Denied")
                }
            )
        }
        retryPermissions.value = false
    }

    if (!cameraPermissionState.value || !storagePermissionState.value) {
        ErrorView(error = Error(
            errorText = Res.string.ocr_capture_permission_denied,
            onRetryClick = { retryPermissions.value = true }
        ))
    } else {
        val cameraKState = rememberCameraKState(
            config = CameraConfiguration(
                cameraLens = CameraLens.BACK,
                flashMode = FlashMode.OFF,

                // Image output
                imageFormat = ImageFormat.PNG,
                directory = Directory.PICTURES,
                qualityPrioritization = QualityPrioritization.BALANCED,

                // iOS only: Advanced camera device types
                cameraDeviceType = CameraDeviceType.DEFAULT,
                aspectRatio = AspectRatio.RATIO_16_9
            )
        )

        val zoomLevel = remember { mutableStateOf(1f) }

        CameraKScreen(
            cameraState = cameraKState.value,
            showPreview = true,
            modifier = Modifier.fillMaxSize(),
            loadingContent = {
                Box(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 50.dp)
                            .size(110.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.background)
                            .border(3.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        LoadingProgressView()
                    }
                }
            },
            errorContent = {
                ErrorView(error = Error(
                    errorText = Res.string.ocr_capture_permission_denied,
                    onRetryClick = { retryPermissions.value = true }
                ))
            }
        ) { readyState ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, _, zoomChange, _ ->
                            zoomLevel.value *= zoomChange
                            zoomLevel.value = zoomLevel.value.coerceIn(1f, 10f)
                            readyState.controller.setZoom(zoomLevel.value)
                        }
                    }
            ) {
                CaptureButton(
                    onClick = { viewModel.takePhoto(readyState.controller) },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 50.dp)
                )
            }
        }
    }
}

@Composable
private fun CaptureButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(110.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.background)
            .border(3.dp, Color.White, CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(Res.drawable.ic_launcher),
            contentDescription = stringResource(Res.string.ocr_capture_btn),
            modifier = Modifier.size(130.dp),
            tint = Color.Unspecified
        )
    }
}
