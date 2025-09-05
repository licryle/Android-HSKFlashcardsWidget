package fr.berliat.hskwidget.ui.OCR

import android.util.Log
import android.widget.Toast
import androidx.annotation.OptIn
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
import fr.berliat.hskwidget.data.dao.AnnotatedChineseWordDAO
import fr.berliat.hskwidget.data.dao.ChineseWordFrequencyDAO
import fr.berliat.hskwidget.data.model.AnnotatedChineseWord
import fr.berliat.hskwidget.data.repo.ChineseWordFrequencyRepo
import fr.berliat.hskwidget.domain.Utils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DisplayOCRViewModelFactory(
    private val createAnnotatedChineseWordDAO: suspend () -> AnnotatedChineseWordDAO,
    private val createChineseWordFrequencyDAO: suspend () -> ChineseWordFrequencyDAO
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DisplayOCRViewModel::class.java)) {
            // Provide a SavedStateHandle manually
            val savedStateHandle = SavedStateHandle()
            @Suppress("UNCHECKED_CAST")
            return DisplayOCRViewModel(savedStateHandle,
                createAnnotatedChineseWordDAO,
                createChineseWordFrequencyDAO) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class DisplayOCRViewModel(private val savedStateHandle: SavedStateHandle,
                          private val createAnnotatedChineseWordDAO: suspend () -> AnnotatedChineseWordDAO,
                          private val createChineseWordFrequencyDAO: suspend () -> ChineseWordFrequencyDAO) : ViewModel() {
    private var _annotatedChineseWordDAO: AnnotatedChineseWordDAO? = null
    suspend fun annotatedChineseWordDAO(): AnnotatedChineseWordDAO = withContext(Dispatchers.IO) {
        if (_annotatedChineseWordDAO == null) {
            _annotatedChineseWordDAO = createAnnotatedChineseWordDAO.invoke()
        }
        return@withContext _annotatedChineseWordDAO!!
    }

    private var _chineseWordFrequencyDAO: ChineseWordFrequencyDAO? = null
    suspend fun chineseWordFrequencyDAO(): ChineseWordFrequencyDAO = withContext(Dispatchers.IO) {
        if (_chineseWordFrequencyDAO == null) {
            _chineseWordFrequencyDAO = createChineseWordFrequencyDAO.invoke()
        }
        return@withContext _chineseWordFrequencyDAO!!
    }

    private var _frequencyWordsRepo : ChineseWordFrequencyRepo? = null
    suspend fun frequencyWordsRepo(): ChineseWordFrequencyRepo = withContext(Dispatchers.IO) {
        if (_frequencyWordsRepo == null) {
            _frequencyWordsRepo = ChineseWordFrequencyRepo(
                chineseWordFrequencyDAO(),
                annotatedChineseWordDAO()
            )
        }

        return@withContext _frequencyWordsRepo!!
    }

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

    suspend fun fetchWord(hanzi: String): AnnotatedChineseWord? = withContext(Dispatchers.IO) {
        Log.d(TAG, "Searching for $hanzi")

        try {
            val word = annotatedChineseWordDAO().getFromSimplified(hanzi)
            Log.d(TAG, "Search returned for $hanzi")

            return@withContext word
        } catch (e: Exception) {
            // Code for handling the exception
            Log.e(TAG, "$e")
            return@withContext null
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
    fun recognizeText(image: suspend () -> InputImage) {
        viewModelScope.launch(Dispatchers.IO) {
            Log.d(TAG, "recognizeText starting")

            val options = ChineseTextRecognizerOptions.Builder()
                .build()

            val recognizer: TextRecognizer = TextRecognition.getClient(options)

            recognizer.process(image.invoke())
                .addOnSuccessListener { visionText ->
                    Log.d(TAG, "Recognized text: ${visionText.text}")
                    viewModelScope.launch(Dispatchers.Default) {
                        processTextRecognitionResult(visionText)
                    }
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
    }

    private suspend fun processTextRecognitionResult(texts: Text) = withContext(Dispatchers.Default) {
        Log.d(TAG, "processTextRecognitionResult")
        val blocks: List<Text.TextBlock> = texts.textBlocks
        if (blocks.isEmpty()) {
            isProcessing.value = false
            toastEvent.value = Pair("No text found", Toast.LENGTH_SHORT)
            return@withContext
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

        text.postValue(text.value + concatText.toString())
    }

    companion object {
        private const val TAG = "DisplayOCRViewModel"
    }
}