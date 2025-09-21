package fr.berliat.hsktextviews.views

import HSKWordView
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp

import fr.berliat.hsktextviews.HSKTextSegmenter
import fr.berliat.hsktextviews.PinyinUtils.getPinyins

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class ShowPinyins(val value: Int) {
    NONE(0),
    CLICKED(1),
    ALL(2);

    companion object {
        fun fromInt(value: Int): ShowPinyins {
            return entries.firstOrNull { it.value == value } ?: NONE
        }
    }
}

@Composable
fun HSKTextView(
    text: String,
    segmenter: HSKTextSegmenter? = null,
    showPinyins: ShowPinyins = ShowPinyins.ALL,
    endSeparator: String = "",
    hanziStyle: TextStyle,
    pinyinStyle: TextStyle,
    clickedWords: Map<String, String>,
    clickedHanziStyle: TextStyle,
    clickedPinyinStyle: TextStyle,
    clickedBackgroundColor: Color = Color.Black,
    loadingComposable: @Composable () -> Unit,
    emptyComposable: @Composable () -> Unit,
    onWordClick: ((String) -> Unit)? = null,
    onTextAnalysisFailure: ((e: Exception) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var words by remember { mutableStateOf(listOf<Pair<String, String>>()) }

    var isLoading by remember { mutableStateOf(true) }
    val scrollState = rememberScrollState()

    val trimText = text.trim()
    LaunchedEffect(trimText, segmenter) {
        if (trimText.isNotBlank()) {
            withContext(Dispatchers.Default) {
                val parsedWords = mutableListOf<Pair<String, String>>()

                trimText.split("\n").forEach { paragraph ->
                    segmenter?.segment(paragraph)?.forEach { word ->
                        parsedWords.add(word to word.getPinyins().joinToString(" "))
                    }
                    parsedWords.add("\n" to "")
                }

                words = parsedWords
            }

            isLoading = false

            if (words.isEmpty()) {
                onTextAnalysisFailure?.invoke(Exception("Segmenter returned no words"))
            }
        }
    }

    when {
        isLoading -> loadingComposable()
        trimText.isBlank() || words.isEmpty() -> emptyComposable()
        else ->
            FlowRow(
                modifier = modifier.fillMaxWidth().verticalScroll(scrollState),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                words.forEach { (word, pinyin) ->
                    if (word == "\n") {
                        Spacer(modifier = Modifier.height(8.dp))
                    } else {
                        val isClicked = clickedWords.keys.contains(word)
                        val truePinyin = if (isClicked) { // Clicked words should contain the truePinyin
                            clickedWords[word]!!
                        } else {
                            pinyin
                        }
                        val showedPinyin = when(showPinyins) {
                            ShowPinyins.NONE -> ""
                            ShowPinyins.CLICKED -> if (isClicked) truePinyin else ""
                            ShowPinyins.ALL -> truePinyin
                        }

                        HSKWordView(
                            modifier = modifier,
                            hanziText = word,
                            pinyinText = showedPinyin,
                            pinyinEditable = false,
                            isClicked = isClicked,
                            onWordClick = { onWordClick?.invoke(word) },
                            hanziStyle = hanziStyle,
                            pinyinStyle = pinyinStyle,
                            clickedHanziStyle = clickedHanziStyle,
                            clickedPinyinStyle = clickedPinyinStyle,
                            clickedBackgroundColor = clickedBackgroundColor
                        )

                        // optional end separator
                        if (endSeparator.isNotEmpty()) {
                            Text(text = endSeparator)
                        }
                    }
                }
            }
    }
}