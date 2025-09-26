package fr.berliat.hskwidget.core

import androidx.core.net.toUri

import co.touchlab.kermit.Logger

import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions

import fr.berliat.hskwidget.ExpectedUtils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import kotlin.text.StringBuilder

actual class HSKOCR actual constructor() {
    actual fun init() {}

    actual suspend fun process(imagePath: String,
                               successCallback: (String?) -> Unit,
                               failureCallBack: (Exception) -> Unit) {
        withContext(Dispatchers.Default) {
            val image = InputImage.fromFilePath(ExpectedUtils.context(), imagePath.toUri())
            Logger.d(tag = TAG, messageString = "recognizeText starting")

            val options = ChineseTextRecognizerOptions.Builder()
                .build()

            val recognizer: TextRecognizer = TextRecognition.getClient(options)

            recognizer.process(image)
                .addOnSuccessListener({ text ->
                    googleTextToString(text, successCallback)
                })
                .addOnFailureListener(failureCallBack)
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
        const val TAG = "HSKOCR"
    }
}