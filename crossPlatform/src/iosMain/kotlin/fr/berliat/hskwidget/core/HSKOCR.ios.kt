package fr.berliat.hskwidget.core

import co.touchlab.kermit.Logger
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.path

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import platform.UIKit.UIImage
import platform.Vision.VNImageRequestHandler
import platform.Vision.VNRecognizedText
import platform.Vision.VNRecognizedTextObservation
import platform.Vision.VNRecognizeTextRequest
import platform.Vision.VNRequestTextRecognitionLevelAccurate

actual class HSKOCR actual constructor() {
	actual fun init() {}

	@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
	actual suspend fun process(
		imagePath: PlatformFile,
		successCallback: (String?) -> Unit,
		failureCallBack: (Exception) -> Unit
	) = withContext(Dispatchers.Main) {
		try {
			Logger.d(tag = TAG, messageString = "recognizeText starting for path: ${imagePath.path}")
			val imagePathString = imagePath.path ?: run {
				failureCallBack(Exception("Image path is null"))
				return@withContext
			}

			val image = UIImage.imageWithContentsOfFile(imagePathString)
			if (image == null) {
				failureCallBack(Exception("Image load failed for path: $imagePathString"))
				return@withContext
			}

			val cgImage = image?.CGImage
			Logger.d(tag = TAG, messageString = "Image loaded, CGImage available")

			val handler = VNImageRequestHandler(cgImage, options = emptyMap<Any?, Any?>())

			val request = VNRecognizeTextRequest { request, error ->
				if (error != null) {
					failureCallBack(Exception(error.localizedDescription))
					return@VNRecognizeTextRequest
				}

				val observations =
					request?.results as? List<VNRecognizedTextObservation> ?: emptyList()

				Logger.d(tag = TAG, messageString = "Found ${observations.size} text observations in image.")

				val recognizedText = observations.joinToString(", ") { observation ->
					val candidates: List<VNRecognizedText> = observation.topCandidates(1u) as List<VNRecognizedText>
					val topCandidate: VNRecognizedText? = candidates.firstOrNull()

					val textResult: String = topCandidate?.string ?: ""
					Logger.d(tag = TAG, messageString = "Candidate string found: $textResult")

					textResult
				}

				Logger.i(tag = TAG, messageString = "OCR extraction complete, length: ${recognizedText.length}")
				successCallback(recognizedText)
			}.apply {
				recognitionLevel = VNRequestTextRecognitionLevelAccurate
				recognitionLanguages = listOf("zh-Hans", "zh-Hant")
			}

			Logger.d(tag = TAG, messageString = "Performing Vision requests")
			handler.performRequests(listOf(request), error = null)
		} catch (e: Exception) {
			failureCallBack(e)
		}
	}

	companion object {
		private const val TAG = "HSKOCR"
	}
}
