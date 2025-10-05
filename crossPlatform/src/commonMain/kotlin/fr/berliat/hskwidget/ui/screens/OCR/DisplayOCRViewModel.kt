package fr.berliat.hskwidget.ui.screens.OCR

import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import co.touchlab.kermit.Logger

import fr.berliat.hsktextviews.HSKTextSegmenter
import fr.berliat.hskwidget.core.Utils
import fr.berliat.hskwidget.core.HSKOCR
import fr.berliat.hskwidget.data.dao.AnnotatedChineseWordDAO
import fr.berliat.hskwidget.data.dao.ChineseWordFrequencyDAO
import fr.berliat.hskwidget.data.model.AnnotatedChineseWord
import fr.berliat.hskwidget.data.repo.ChineseWordFrequencyRepo
import fr.berliat.hskwidget.data.store.AppPreferencesStore
import fr.berliat.hskwidget.ui.components.smallestHanziFontSize

import hskflashcardswidget.crossplatform.generated.resources.Res
import hskflashcardswidget.crossplatform.generated.resources.ocr_display_no_text_found
import hskflashcardswidget.crossplatform.generated.resources.ocr_display_ocr_failed
import hskflashcardswidget.crossplatform.generated.resources.ocr_display_smallest_text
import hskflashcardswidget.crossplatform.generated.resources.ocr_display_word_not_found

import io.github.vinceglb.filekit.PlatformFile

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import org.jetbrains.compose.resources.StringResource

import kotlin.time.Duration.Companion.milliseconds

class DisplayOCRViewModel(
    private val appPreferences: AppPreferencesStore,
    private val annotatedChineseWordDAO: AnnotatedChineseWordDAO,
                chineseWordFrequencyDAO: ChineseWordFrequencyDAO,
    private val segmenter : HSKTextSegmenter) : ViewModel() {
    private val frequencyWordsRepo: ChineseWordFrequencyRepo = ChineseWordFrequencyRepo(
                chineseWordFrequencyDAO,
                annotatedChineseWordDAO
            )

    val showPinyins = appPreferences.readerShowAllPinyins.asStateFlow()
    val separatorEnabled = appPreferences.readerSeparateWords.asStateFlow()
    val textSize = appPreferences.readerTextSize.asStateFlow()

    private val _text = MutableStateFlow("")
    val text: StateFlow<String> = _text
    fun setText(s: String) { _text.value = s }

    private val _error = MutableStateFlow<StringResource?>(null)
    val error: StateFlow<StringResource?> = _error
    fun setError(e: StringResource) { _error.value = e }

    private val _selectedWord = MutableStateFlow<AnnotatedChineseWord?>(null)
    val selectedWord: StateFlow<AnnotatedChineseWord?> = _selectedWord

    private val _clickedWords = MutableStateFlow<Map<String, String>>(emptyMap())
    val clickedWords: StateFlow<Map<String, String>> = _clickedWords

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing

    private val _isSegmenterReady = MutableStateFlow(false)
    val isSegmenterReady: StateFlow<Boolean> = _isSegmenterReady

    val wordSeparator = WORD_SEPARATOR

    init {
        viewModelScope.launch(Dispatchers.IO) {
            selectedWord.value?.let {
                fetchWordForDisplay(it.simplified)
            }

            while (!segmenter.isReady()) {
                // Do something repeatedly
                println("Still waiting for segmenter to come online")

                // Wait before checking again (important — don’t busy loop!)
                delay(100.milliseconds)
            }

            _isSegmenterReady.value = true
        }
    }

    fun resetText() {
        _text.value = ""
        _selectedWord.value = null
        _clickedWords.value = emptyMap()
    }

    // TODO reconnect
    fun augmentWordFrequencyAppeared(words: Map<String, Int>) {
        viewModelScope.launch(Dispatchers.IO) {
            frequencyWordsRepo.incrementAppeared(words)
        }
    }

    fun augmentWordFrequencyConsulted(hanzi: String) {
        viewModelScope.launch(Dispatchers.IO) {
            frequencyWordsRepo.incrementConsulted(hanzi)
        }
    }

    suspend fun fetchWord(hanzi: String): AnnotatedChineseWord? = withContext(Dispatchers.IO) {
        Logger.d(tag = TAG, messageString = "Searching for $hanzi")

        try {
            val word = annotatedChineseWordDAO.getFromSimplified(hanzi)
            Logger.d(tag = TAG, messageString = "Search returned for $hanzi")

            return@withContext word
        } catch (e: Exception) {
            // Code for handling the exception
            Logger.e(tag = TAG, messageString = "$e")
            return@withContext null
        }
    }

    fun fetchWordForDisplay(simplified: String) {
        viewModelScope.launch(Dispatchers.IO) {
            // Switch to the IO dispatcher to perform background work
            val annotatedWord = fetchWord(simplified)

            if (annotatedWord == null) {
                Utils.toast(Res.string.ocr_display_word_not_found)

                withContext(Dispatchers.Main) {
                    Utils.copyToClipBoard(simplified)
                }

                Utils.logAnalyticsEvent(Utils.ANALYTICS_EVENTS.OCR_WORD_NOTFOUND)
            } else {
                _selectedWord.value = annotatedWord
                val pinyin = ((annotatedWord.word?.pinyins ?: annotatedWord.annotation?.pinyins) ?: "").toString()
                _clickedWords.update { old -> old + (annotatedWord.simplified to pinyin) }

                augmentWordFrequencyConsulted(annotatedWord.simplified)
                Utils.logAnalyticsEvent(Utils.ANALYTICS_EVENTS.OCR_WORD_FOUND)
            }
        }
    }

    fun updateTextSize(increment: Float) {
        val textSize = (appPreferences.readerTextSize.value.value + increment).coerceAtLeast(smallestHanziFontSize.value)

        if (textSize == smallestHanziFontSize.value) {
            viewModelScope.launch {
                Utils.toast(Res.string.ocr_display_smallest_text)
            }
        }

        Logger.d(tag = TAG, messageString = "updateTextSize to $textSize")
        appPreferences.readerTextSize.value = textSize.sp
    }

    fun toggleShowPinyins(showPinyins: Boolean) {
        appPreferences.readerShowAllPinyins.value = showPinyins
    }

    fun toggleSeparator(showSeparator: Boolean) {
        appPreferences.readerSeparateWords.value = showSeparator
    }

    fun copyToClipboard(word: AnnotatedChineseWord) {
        Utils.copyToClipBoard(word.simplified)
    }

    fun speakWord(word: AnnotatedChineseWord) {
        Utils.playWordInBackground(word.simplified)
    }

    fun recognizeText(imagePath: PlatformFile) {
        viewModelScope.launch(Dispatchers.IO) {
            HSKOCR().process(imagePath, { text ->
                Logger.d(tag = TAG, messageString = "Recognized text: $text")

                if (text == null) {
                    _error.value = Res.string.ocr_display_no_text_found
                } else {
                    var newText = text
                    if (_text.value.isNotEmpty()) newText = "\n\n" + text

                    _text.update { _text.value + newText }
                }
                _isProcessing.value = false
            }, { e ->
                _error.value = Res.string.ocr_display_ocr_failed
                _isProcessing.value = false

                Logger.e(tag = TAG, messageString = "Text recognition failed: " + e.message)

                Utils.logAnalyticsError(
                    "OCR_DISPLAY",
                    "TextRecognitionFailed",
                    e.message ?: ""
                )
            })
        }
    }

    companion object {
        private const val TAG = "DisplayOCRViewModel"

        const val WORD_SEPARATOR = "·"
    }
}