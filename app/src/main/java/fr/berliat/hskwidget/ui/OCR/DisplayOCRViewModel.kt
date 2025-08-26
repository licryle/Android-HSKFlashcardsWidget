package fr.berliat.hskwidget.ui.OCR

import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.ExperimentalGetImage
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import fr.berliat.hskwidget.data.model.AnnotatedChineseWord
import fr.berliat.hskwidget.data.repo.ChineseWordFrequencyRepo
import fr.berliat.hskwidget.data.store.ChineseWordsDatabase
import fr.berliat.hskwidget.domain.Utils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DisplayOCRViewModelFactory(
    private val application: AppCompatActivity
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DisplayOCRViewModel::class.java)) {
            // Provide a SavedStateHandle manually
            val savedStateHandle = SavedStateHandle()
            @Suppress("UNCHECKED_CAST")
            return DisplayOCRViewModel(savedStateHandle, application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class DisplayOCRViewModel(private val savedStateHandle: SavedStateHandle, val application: AppCompatActivity) : ViewModel() {

    val clickedWords: MutableMap<String, String> = mutableMapOf()
    var selectedWord: String? = null

    // Key for scroll position
    private val SCROLL_POSITION_KEY = "scroll_position"
    val toastEvent = MutableLiveData(Pair("", Toast.LENGTH_LONG))

    // Getter and Setter for scroll position
    var scrollPosition: Int
        get() = savedStateHandle.get<Int>(SCROLL_POSITION_KEY) ?: 0
        set(value) {
            savedStateHandle.set(SCROLL_POSITION_KEY, value)
        }

    val text = MutableLiveData("")
    val isProcessing = MutableLiveData(false)

    fun resetText() {
        scrollPosition = 0
        text.value = ""
        selectedWord = null
        clickedWords.clear()
    }

    suspend fun frequencyWordsRepo(): ChineseWordFrequencyRepo {
        val db = ChineseWordsDatabase.getInstance(application)
        return ChineseWordFrequencyRepo(
            db.chineseWordFrequencyDAO(),
            db.annotatedChineseWordDAO()
        )
    }

    fun augmentWordFrequencyAppeared(words: Map<String, Int>) {
        viewModelScope.launch(Dispatchers.IO) {
            frequencyWordsRepo().incrementAppeared(words)
        }
    }

    fun augmentWordFrequencyConsulted(hanzi: String) {
        viewModelScope.launch(Dispatchers.IO) {
            frequencyWordsRepo().incrementConsulted(hanzi)
        }
    }

    suspend fun fetchWord(hanzi: String): AnnotatedChineseWord? {
        Log.d(TAG, "Searching for $hanzi")
        val db = ChineseWordsDatabase.getInstance(application)
        val dao = db.annotatedChineseWordDAO()
        try {
            val word = dao.getFromSimplified(hanzi)
            Log.d(TAG, "Search returned for $hanzi")

            return word
        } catch (e: Exception) {
            // Code for handling the exception
            Log.e(TAG, "$e")
            return null
        }
    }

    fun fetchWordForDisplay(simplified: String, callback: (String, AnnotatedChineseWord?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            // Switch to the IO dispatcher to perform background work
            val result = fetchWord(simplified)

            withContext(Dispatchers.Main) {
                callback(simplified, result)
            }
        }
    }

    @OptIn(ExperimentalGetImage::class)
    fun recognizeText(uri: Uri) {
        Log.d(TAG, "recognizeText starting")
        // Convert the Uri to InputImage for OCR
        val image = InputImage.fromFilePath(application, uri)

        val options = ChineseTextRecognizerOptions.Builder()
            .build()

        val recognizer: TextRecognizer = TextRecognition.getClient(options)

        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                processTextRecognitionResult(visionText)
                Log.d(TAG, "Recognized text: ${visionText.text}")
            }
            .addOnFailureListener { e ->
                isProcessing.value = false
                toastEvent.value = Pair("Text recognition failed", Toast.LENGTH_LONG)
                Log.e(TAG, "Text recognition failed: ", e)
                e.printStackTrace()

                Utils.logAnalyticsError(
                    "OCR_DISPLAY",
                    "TextRecognitionFailed",
                    e.message ?: ""
                )
            }
    }

    private fun processTextRecognitionResult(texts: Text) {
        Log.d(TAG, "processTextRecognitionResult")
        val blocks: List<Text.TextBlock> = texts.textBlocks
        if (blocks.isEmpty()) {
            isProcessing.value = false
            toastEvent.value = Pair("No text found", Toast.LENGTH_SHORT)
            return
        }

        val concatText = StringBuilder()
        for (i in blocks.indices) {
            val lines: List<Text.Line> = blocks[i].lines
            for (j in lines.indices) {
                val elements: List<Text.Element> = lines[j].elements
                for (k in elements.indices) {
                    Log.d(TAG, elements[k].text)
                    concatText.append(elements[k].text)
                }
                Log.d(TAG, "END OF LINE")
                concatText.append("\n\n")
            }
            Log.d(TAG, "END OF BLOCK")
        }

        Log.i(TAG, "Text recognition extracted, moving to display fragment: \n$concatText")

        if (text.value?.isNotEmpty() == true)
            concatText.insert(0, "\n\n")

        text.value += concatText.toString()
    }

    companion object {
        private const val TAG = "DisplayOCRViewModel"
    }
}