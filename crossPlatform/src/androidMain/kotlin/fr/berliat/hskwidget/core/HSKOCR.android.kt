package fr.berliat.hskwidget.core

import android.graphics.BitmapFactory
import co.touchlab.kermit.Logger

import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.path

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import kotlin.text.StringBuilder
import androidx.core.net.toUri

actual class HSKOCR actual constructor() {
    actual fun init() {}

    actual suspend fun process(imagePath: PlatformFile,
                               successCallback: (String?) -> Unit,
                               failureCallBack: (Exception) -> Unit) {
        withContext(Dispatchers.Default) {
            try {
                Logger.d(tag = TAG, messageString = "recognizeText starting for path: ${imagePath.path}")
                val uri = imagePath.path.toUri()
                val image = if (uri.scheme != null) {
                    Logger.d(tag = TAG, messageString = "Loading image from URI: $uri")
                    InputImage.fromFilePath(ExpectedUtils.context, uri)
                } else {
                    Logger.d(tag = TAG, messageString = "Loading image from file path: ${imagePath.path}")
                    val bitmap = BitmapFactory.decodeFile(imagePath.path)
                    InputImage.fromBitmap(bitmap, 0)
                }
                Logger.d(tag = TAG, messageString = "recognizeText image loaded successfully")

                val options = ChineseTextRecognizerOptions.Builder()
                    .build()

                val recognizer: TextRecognizer = TextRecognition.getClient(options)

                Logger.d(tag = TAG, messageString = "Starting ML Kit text recognition")
                recognizer.process(image)
                    .addOnSuccessListener({ text ->
                        Logger.d(tag = TAG, messageString = "ML Kit success: found ${text.textBlocks.size} blocks")
                        googleTextToString(text, successCallback)
                    })
                    .addOnFailureListener(failureCallBack)
            } catch (e: Exception) {
                failureCallBack.invoke(e)
            }
        }
    }

    private fun googleTextToString(text: Text, callBack: (String?) -> Unit) {
        Logger.d(tag = TAG, messageString = "processTextRecognitionResult")
        val blocks: List<Text.TextBlock> = text.textBlocks
        if (blocks.isEmpty()) {
            callBack(null)
            return
        }

        val concatText = StringBuilder()
        for (i in blocks.indices) {
            val lines: List<Text.Line> = blocks[i].lines
            for (j in lines.indices) {
                val elements: List<Text.Element> = lines[j].elements
                for (k in elements.indices) {
                    Logger.d(tag = TAG, messageString = elements[k].text)
                    concatText.append(elements[k].text)
                }
                Logger.d(tag = TAG, messageString = "END OF LINE")
                concatText.append("\n\n")
            }
            Logger.d(tag = TAG, messageString = "END OF BLOCK")
        }

        Logger.i(tag = TAG, messageString = "Text recognition extracted, moving to display fragment: \n$concatText")

        callBack(concatText.toString())
    }

    companion object {
        private const val TAG = "HSKOCR"
    }
}