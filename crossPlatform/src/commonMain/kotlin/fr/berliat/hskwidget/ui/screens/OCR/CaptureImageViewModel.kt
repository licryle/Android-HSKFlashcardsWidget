package fr.berliat.hskwidget.ui.screens.OCR

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger

import com.kashif.cameraK.controller.CameraController
import com.kashif.cameraK.result.ImageCaptureResult
import kotlinx.coroutines.launch

import fr.berliat.hskwidget.core.Utils
import fr.berliat.hskwidget.core.YYMMDDHHMMSS
import fr.berliat.hskwidget.core.toSafeFileName

import fr.berliat.hskwidget.Res
import fr.berliat.hskwidget.core.AppDispatchers
import fr.berliat.hskwidget.ocr_capture_error_processed
import fr.berliat.hskwidget.ocr_capture_error_save

import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.cacheDir
import io.github.vinceglb.filekit.div
import io.github.vinceglb.filekit.write

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock

class CaptureImageViewModel(
    val onImageReady: (PlatformFile) -> Unit
): ViewModel() {
    val _isProcessing = MutableStateFlow<Boolean>(true)
    val isProcessing = _isProcessing.asStateFlow()

    val _cameraController = MutableStateFlow<CameraController?>(null)
    val cameraController = _cameraController.asStateFlow()

    fun takePhoto() {
        cameraController.value?.let {
            _isProcessing.value = true
            viewModelScope.launch(AppDispatchers.IO) {
                when (val result = it.takePicture()) {
                    is ImageCaptureResult.Success -> {
                        val file = FileKit.cacheDir / "Photo_${
                            Clock.System.now().YYMMDDHHMMSS().toSafeFileName()
                        }.jpg"

                        try {
                            file.write(result.byteArray)
                        } catch (_: Exception) {
                            Utils.toast(Res.string.ocr_capture_error_save)
                        } finally {
                            _isProcessing.value = false
                        }

                        withContext(Dispatchers.Main) {
                            onImageReady(file)
                        }
                    }

                    is ImageCaptureResult.Error -> {
                        Logger.e(
                            tag = TAG,
                            messageString = "Image Capture Error: ${result.exception.message}"
                        )
                        Utils.toast(Res.string.ocr_capture_error_processed)

                        Utils.logAnalyticsError(
                            "OCR_CAPTURE",
                            "ProcessingPictureFailed",
                            result.exception.message ?: ""
                        )
                        _isProcessing.value = false
                    }
                }
            }
        }

        Utils.logAnalyticsEvent(Utils.ANALYTICS_EVENTS.OCR_CAPTURE)
    }

    fun onCameraControllerReady(controller: CameraController) {
        _cameraController.value?.stopSession()
        _cameraController.value = controller
        _isProcessing.value = false
    }

    override fun onCleared() {
        super.onCleared()
        _cameraController.value = null
    }

    companion object {
        private const val TAG = "CaptureImageViewModel"
    }
}
