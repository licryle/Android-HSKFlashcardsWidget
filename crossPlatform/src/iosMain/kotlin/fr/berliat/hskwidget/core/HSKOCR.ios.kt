package fr.berliat.hskwidget.core

import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.path

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.cinterop.useContents

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

			val handler = VNImageRequestHandler(cgImage, options = emptyMap<Any?, Any?>())

			val request = VNRecognizeTextRequest { request, error ->
				if (error != null) {
					return@VNRecognizeTextRequest
				}

				val observations =
					request?.results as? List<VNRecognizedTextObservation> ?: emptyList()

				println("DEBUG: Found ${observations.size} text observations in image.")

				val recognizedText = observations.joinToString(", ") { observation ->
					println("DEBUG: reading observation bounds: X=${observation.boundingBox.useContents { origin.x }}")

					val candidates: List<VNRecognizedText> = observation.topCandidates(1u) as List<VNRecognizedText>
					val topCandidate: VNRecognizedText? = candidates.firstOrNull()

					val textResult: String = topCandidate?.string ?: "[Text Not Found]"

					println("DEBUG: Candidate string found: $textResult")

					textResult
				}

				successCallback(recognizedText)
			}.apply {
				recognitionLevel = VNRequestTextRecognitionLevelAccurate
				recognitionLanguages = listOf("zh-Hans", "zh-Hant")
			}

			handler.performRequests(listOf(request), error = null)
		} catch (e: Exception) {
			failureCallBack(e)
		}
	}
}
