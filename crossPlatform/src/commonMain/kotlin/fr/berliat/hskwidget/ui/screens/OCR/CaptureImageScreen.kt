package fr.berliat.hskwidget.ui.screens.OCR

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

import co.touchlab.kermit.Logger

import com.kashif.cameraK.enums.CameraLens
import com.kashif.cameraK.enums.Directory
import com.kashif.cameraK.enums.FlashMode
import com.kashif.cameraK.enums.ImageFormat
import com.kashif.cameraK.enums.PinchToZoom
import com.kashif.cameraK.enums.QualityPrioritization
import com.kashif.cameraK.permissions.providePermissions
import com.kashif.cameraK.ui.CameraPreview

import fr.berliat.hskwidget.ui.components.Error
import fr.berliat.hskwidget.ui.components.ErrorView
import fr.berliat.hskwidget.ui.components.LoadingProgressView

import hskflashcardswidget.crossplatform.generated.resources.Res
import hskflashcardswidget.crossplatform.generated.resources.ic_launcher
import hskflashcardswidget.crossplatform.generated.resources.ocr_capture_btn
import hskflashcardswidget.crossplatform.generated.resources.ocr_capture_permission_denied

import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private const val TAG = "CaptureImageScreen"

@Composable
fun CaptureImageScreen(
    modifier: Modifier = Modifier,
    viewModel: CaptureImageViewModel = CaptureImageViewModel {}
) {
    val isProcessing by viewModel.isProcessing.collectAsState()

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
        Column(modifier = modifier.fillMaxSize()) {
            Box(modifier = Modifier.weight(1f)) {
                CameraPreview(
                    modifier = Modifier.fillMaxSize(),
                    cameraConfiguration = {
                        setCameraLens(CameraLens.BACK)
                        setFlashMode(FlashMode.OFF)
                        setImageFormat(ImageFormat.PNG)
                        setDirectory(Directory.PICTURES)
                        setPinchToZoom(PinchToZoom.ON)
                        setQualityPrioritization(QualityPrioritization.BALANCED)
                    },
                    onCameraControllerReady = viewModel::onCameraControllerReady
                )

                if (isProcessing) {
                    Box(
                        modifier = modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 50.dp)
                            .size(110.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.background) // or launcher background
                            .border(3.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        LoadingProgressView()
                    }
                } else {
                    CaptureButton(
                        onClick = { viewModel.takePhoto() },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 50.dp)
                    )
                }
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
            .background(MaterialTheme.colorScheme.background) // or launcher background
            .border(3.dp, Color.White, CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(Res.drawable.ic_launcher),
            contentDescription = stringResource(Res.string.ocr_capture_btn),
            modifier = Modifier.size(130.dp),
            tint = Color.Unspecified // keep raw drawable colors
        )
    }
}