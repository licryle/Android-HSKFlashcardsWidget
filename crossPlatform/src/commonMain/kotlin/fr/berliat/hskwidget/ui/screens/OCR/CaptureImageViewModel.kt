package fr.berliat.hskwidget.ui.screens.OCR

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger

import com.kashif.cameraK.controller.CameraController
import com.kashif.cameraK.result.ImageCaptureResult
import com.kashif.imagesaverplugin.ImageSaverPlugin

import fr.berliat.hskwidget.Utils
import fr.berliat.hskwidget.YYMMDDHHMMSS
import hskflashcardswidget.crossplatform.generated.resources.Res
import hskflashcardswidget.crossplatform.generated.resources.ocr_capture_error_processed
import hskflashcardswidget.crossplatform.generated.resources.ocr_capture_error_save

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

class CaptureImageViewModel(
    val onImageReady: (String) -> Unit
): ViewModel() {
    val _isProcessing = MutableStateFlow<Boolean>(true)
    val isProcessing = _isProcessing.asStateFlow()

    val _cameraController = MutableStateFlow<CameraController?>(null)
    val cameraController = _cameraController.asStateFlow()

    fun takePhoto(imageSaverPlugin: ImageSaverPlugin) {
        Utils.logAnalyticsEvent(Utils.ANALYTICS_EVENTS.OCR_CAPTURE)

        cameraController.value?.let {
            _isProcessing.value = true
            viewModelScope.launch(Dispatchers.IO) {
                when (val result = it.takePicture()) {
                    is ImageCaptureResult.Success -> {
                        val filePath = imageSaverPlugin.saveImage(
                            // TODO reinstate Crop
                            byteArray = result.byteArray,
                            imageName = "Photo_" + Clock.System.now().YYMMDDHHMMSS()
                        )

                        if (filePath == null) {
                            Utils.toast(Res.string.ocr_capture_error_save)
                        } else {
                            onImageReady(filePath)
                        }
                        _isProcessing.value = false
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
    }

    fun onCameraControllerReady(controller: CameraController) {
        _cameraController.value = controller
        _isProcessing.value = false
    }

    companion object {
        private const val TAG = "CaptureImageViewModel"
    }
}
