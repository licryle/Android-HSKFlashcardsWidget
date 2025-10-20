package fr.berliat.hskwidget.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontStyle.Companion.Italic

import fr.berliat.hskwidget.core.Locale
import fr.berliat.hskwidget.core.Logging
import fr.berliat.hskwidget.data.model.AnnotatedChineseWord
import fr.berliat.hskwidget.data.type.Modality
import fr.berliat.hskwidget.data.type.WordType
import fr.berliat.hskwidget.core.capitalize
import fr.berliat.hskwidget.data.type.HSK_Level
import fr.berliat.hskwidget.ui.theme.AppTypographies
import fr.berliat.hskwidget.Res
import fr.berliat.hskwidget.baseline_volume_up_24
import fr.berliat.hskwidget.bookmark_24px
import fr.berliat.hskwidget.bookmark_heart_24px
import fr.berliat.hskwidget.content_copy_24px
import fr.berliat.hskwidget.dictionary_item_altdefinition
import fr.berliat.hskwidget.dictionary_item_annotate
import fr.berliat.hskwidget.dictionary_item_antonym
import fr.berliat.hskwidget.dictionary_item_examples
import fr.berliat.hskwidget.dictionary_item_lists
import fr.berliat.hskwidget.dictionary_item_modality
import fr.berliat.hskwidget.dictionary_item_synonyms
import fr.berliat.hskwidget.dictionary_item_toggle
import fr.berliat.hskwidget.dictionary_item_type
import fr.berliat.hskwidget.format_list_bulleted_add_24px
import fr.berliat.hskwidget.keyboard_arrow_down_24px
import fr.berliat.hskwidget.keyboard_arrow_up_24px
import fr.berliat.hskwidget.widget_btn_copy
import fr.berliat.hskwidget.widget_btn_speak

import HSKWordView

import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun DetailedWordView (
    word: AnnotatedChineseWord,
    showHSK3Definition: Boolean,
    pinyinEditable: Boolean,
    shapeModifier: PrettyCardShapeModifier,
    modifier: Modifier = Modifier,
    onFavoriteClick: ((AnnotatedChineseWord) -> Unit)? = null,
    onSpeakClick: ((AnnotatedChineseWord) -> Unit)? = null,
    onCopyClick: ((AnnotatedChineseWord) -> Unit)? = null,
    onListsClick: ((AnnotatedChineseWord) -> Unit)? = null,
    onPinyinChange: (String) -> Unit = {}
) {
    // Compute definition / annotation / alt definition
    var definition = word.word?.definition?.get(Locale.ENGLISH) ?: ""
    var annotation = word.annotation?.notes ?: ""
    if (definition.isEmpty()) {
        definition = word.annotation?.notes ?: ""
        annotation = ""
    }
    var altDef = word.word?.definition?.get(Locale.CN_HSK3) ?: ""

    if (showHSK3Definition && altDef.isNotEmpty()) {
        val tmp = altDef
        altDef = definition
        definition = tmp
    }

    // Pinyins
    var pinyins = word.word?.pinyins.toString().ifEmpty { word.annotation?.pinyins?.toString() ?: "" }

    // Show/Hide "more" section
    var isMoreVisible by remember { mutableStateOf(false) }
    val nothingMore = (altDef + (word.word?.examples ?: "") + (word.word?.antonym ?: "") + (word.word?.synonyms ?: ""))
        .isEmpty() &&
            (word.word?.modality ?: Modality.UNKNOWN) == Modality.UNKNOWN &&
            (word.word?.wordType ?: WordType.UNKNOWN) == WordType.UNKNOWN

    PrettyCard(
        onClick = {
            if (nothingMore) return@PrettyCard

            isMoreVisible = !isMoreVisible
            if (isMoreVisible)
                Logging.logAnalyticsEvent(Logging.ANALYTICS_EVENTS.WIDGET_EXPAND)
            else
                Logging.logAnalyticsEvent(Logging.ANALYTICS_EVENTS.WIDGET_COLLAPSE)
        },
        shapeModifier = shapeModifier
    ) {
        Column {
            Row(
                modifier = modifier.fillMaxWidth().height(IntrinsicSize.Max),
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = modifier.fillMaxHeight().wrapContentWidth(),
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    onSpeakClick?.let {
                        RoundIconButton(
                            Res.drawable.baseline_volume_up_24,
                            Res.string.widget_btn_speak,
                            { onSpeakClick(word) }
                        )
                    }

                    onCopyClick?.let {
                        RoundIconButton(
                            Res.drawable.content_copy_24px,
                            Res.string.widget_btn_copy,
                            { onCopyClick(word) }
                        )
                    }
                }

                val vSpacing = if (nothingMore) Arrangement.Top else Arrangement.SpaceBetween
                Column(modifier = modifier.weight(1f).fillMaxHeight(),
                    verticalArrangement = vSpacing) {
                    HSKWordView(
                        hanziText = word.simplified,
                        pinyinText = pinyins,
                        pinyinEditable = pinyinEditable && !word.hasWord(),
                        onPinyinChange = {
                            if (pinyins != it) {
                                pinyins = it
                                onPinyinChange(it)
                            }
                        },
                        hanziStyle = AppTypographies.hanzi,
                        pinyinStyle = AppTypographies.pinyin,
                        clickedHanziStyle = AppTypographies.clickedHanzi,
                        clickedPinyinStyle = AppTypographies.clickedPinyin,
                    )
                    // Definition & Annotation
                    Text(
                        definition.ifEmpty { annotation },
                        style = MaterialTheme.typography.bodyLarge
                    )
                    if (!definition.isEmpty() && annotation.isNotEmpty()) {
                        Text(
                            annotation,
                            style = MaterialTheme.typography.bodyMedium.copy(fontStyle = Italic),
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }

                    // Toggle more
                    if (!nothingMore) {
                        Row(horizontalArrangement = Arrangement.Center,
                            modifier = modifier.fillMaxWidth()) {
                            Icon(
                                painter = painterResource(
                                    if (isMoreVisible) Res.drawable.keyboard_arrow_up_24px
                                    else Res.drawable.keyboard_arrow_down_24px
                                ),
                                contentDescription = stringResource(Res.string.dictionary_item_toggle)
                            )
                        }
                    }
                }

                Column(
                    modifier = modifier.fillMaxHeight().wrapContentWidth(),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.End
                ) {
                    onFavoriteClick?.let {
                        IconButton(onClick = { onFavoriteClick(word) }) {
                            val resId =
                                if (word.hasAnnotation()) Res.drawable.bookmark_heart_24px else Res.drawable.bookmark_24px
                            Icon(
                                painter = painterResource(resId),
                                contentDescription = stringResource(Res.string.dictionary_item_annotate),
                                tint = if (word.hasAnnotation()) MaterialTheme.colorScheme.primary else Color.Gray
                            )
                        }
                    }

                    val hSK = word.word?.let {
                        (it.hskLevel?.takeIf{ lvl -> lvl != HSK_Level.NOT_HSK }  ?: "").toString()
                    } ?: ""
                    Text(
                        text = hSK,
                        modifier = Modifier.padding(end = 8.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                    )

                    onListsClick?.let {
                        RoundIconButton(
                            Res.drawable.format_list_bulleted_add_24px,
                            Res.string.dictionary_item_lists,
                            { onListsClick(word) }
                        )
                    }
                }
            }

            if (isMoreVisible) {
                // More section
                Row(
                    modifier = modifier.padding(10.dp)
                ) {
                    Column(modifier = modifier.weight(1f)) {
                        val modality = word.word?.modality ?: Modality.UNKNOWN
                        if (modality != Modality.UNKNOWN) {
                            Text(
                                stringResource(Res.string.dictionary_item_modality),
                                style = AppTypographies.detailCardSubTitle)
                            Text(
                                modality.toString().capitalize(),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        word.word?.synonyms?.takeIf { it.isNotEmpty() }?.let {
                            Text(
                                stringResource(Res.string.dictionary_item_synonyms),
                                style = AppTypographies.detailCardSubTitle)
                            Text(it, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    Column(modifier = modifier.weight(1f), horizontalAlignment = Alignment.End) {
                        val wordType = word.word?.wordType ?: WordType.UNKNOWN
                        if (wordType != WordType.UNKNOWN) {
                            Text(
                                stringResource(Res.string.dictionary_item_type),
                                style = AppTypographies.detailCardSubTitle)
                            Text(
                                wordType.toString().capitalize(),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        word.word?.antonym?.takeIf { it.isNotEmpty() }?.let {
                            Text(
                                stringResource(Res.string.dictionary_item_antonym),
                                style = AppTypographies.detailCardSubTitle)
                            Text(it, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                Row(
                    modifier = modifier.padding(10.dp)
                ) {
                    Column {
                        Text(
                            stringResource(Res.string.dictionary_item_examples),
                            style = AppTypographies.detailCardSubTitle)

                        word.word?.examples?.takeIf { it.isNotEmpty() }?.let {
                            Text(it, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                Row(
                    modifier = modifier.padding(10.dp)
                ) {
                    Column {
                        Text(
                            stringResource(Res.string.dictionary_item_altdefinition),
                            style = AppTypographies.detailCardSubTitle)

                        if (altDef.isNotEmpty()) {
                            Text(altDef, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}
