package fr.berliat.hskwidget.ui.components

import org.jetbrains.compose.resources.painterResource

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.ui.Alignment

import fr.berliat.hskwidget.core.Locale
import fr.berliat.hskwidget.data.model.AnnotatedChineseWord
import fr.berliat.hskwidget.data.type.Modality
import fr.berliat.hskwidget.data.type.WordType
import fr.berliat.hskwidget.capitalize
import fr.berliat.hskwidget.ANALYTICS_EVENTS
import fr.berliat.hskwidget.Utils
import fr.berliat.hskwidget.data.type.HSK_Level

import HSKWordView

import hskflashcardswidget.crossplatform.generated.resources.Res
import hskflashcardswidget.crossplatform.generated.resources.baseline_volume_up_24
import hskflashcardswidget.crossplatform.generated.resources.bookmark_24px
import hskflashcardswidget.crossplatform.generated.resources.bookmark_heart_24px
import hskflashcardswidget.crossplatform.generated.resources.content_copy_24px
import hskflashcardswidget.crossplatform.generated.resources.dictionary_item_altdefinition
import hskflashcardswidget.crossplatform.generated.resources.dictionary_item_annotate
import hskflashcardswidget.crossplatform.generated.resources.dictionary_item_antonym
import hskflashcardswidget.crossplatform.generated.resources.dictionary_item_examples
import hskflashcardswidget.crossplatform.generated.resources.dictionary_item_lists
import hskflashcardswidget.crossplatform.generated.resources.dictionary_item_synonyms
import hskflashcardswidget.crossplatform.generated.resources.dictionary_item_toggle
import hskflashcardswidget.crossplatform.generated.resources.format_list_bulleted_add_24px
import hskflashcardswidget.crossplatform.generated.resources.keyboard_arrow_down_24px
import hskflashcardswidget.crossplatform.generated.resources.keyboard_arrow_up_24px
import hskflashcardswidget.crossplatform.generated.resources.widget_btn_copy
import hskflashcardswidget.crossplatform.generated.resources.widget_btn_speak

import org.jetbrains.compose.resources.stringResource

@Composable
fun DetailedWordView (
    word: AnnotatedChineseWord,
    showHSK3Definition: Boolean,
    onFavoriteClick: (() -> Unit)? = null,
    onSpeakClick: (() -> Unit)? = null,
    onCopyClick: (() -> Unit)? = null,
    onListsClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
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
    val pinyins = word.word?.pinyins.toString().ifEmpty { word.annotation?.pinyins?.toString() ?: "" }

    // Show/Hide "more" section
    var isMoreVisible by remember { mutableStateOf(false) }
    val nothingMore = (altDef + (word.word?.examples ?: "") + (word.word?.antonym ?: "") + (word.word?.synonyms ?: ""))
        .isEmpty() &&
            (word.word?.modality ?: Modality.UNKNOWN) == Modality.UNKNOWN &&
            (word.word?.wordType ?: WordType.UNKNOWN) == WordType.UNKNOWN

    Card(
        modifier = modifier.fillMaxWidth().wrapContentHeight()
            .border(
                width = 0.5.dp,
                color = Color.Gray, // choose your color
                shape = RoundedCornerShape(8.dp) // match Card shape
            ),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        onClick = {
            if (nothingMore) return@Card

            isMoreVisible = !isMoreVisible
            if (isMoreVisible)
                Utils.logAnalyticsEvent(ANALYTICS_EVENTS.WIDGET_EXPAND)
            else
                Utils.logAnalyticsEvent(ANALYTICS_EVENTS.WIDGET_COLLAPSE)
        }
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
                            onSpeakClick
                        )
                    }

                    onCopyClick?.let {
                        RoundIconButton(
                            Res.drawable.content_copy_24px,
                            Res.string.widget_btn_copy,
                            onCopyClick
                        )
                    }
                }

                Column(modifier = modifier.weight(1f).fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceBetween) {
                    HSKWordView(
                        hanziText = word.simplified,
                        pinyinText = pinyins
                    )
                    // Definition & Annotation
                    Text(
                        definition.ifEmpty { annotation },
                        style = MaterialTheme.typography.bodyLarge
                    )
                    if (!definition.isEmpty() && annotation.isNotEmpty()) {
                        Text(
                            annotation,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondary
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
                        IconButton(onClick = onFavoriteClick) {
                            val resId =
                                if (word.hasAnnotation()) Res.drawable.bookmark_heart_24px else Res.drawable.bookmark_24px
                            Icon(
                                painter = painterResource(resId),
                                contentDescription = stringResource(Res.string.dictionary_item_annotate),
                                tint = if (word.hasAnnotation()) Color.Red else Color.Gray
                            )
                        }
                    }

                    val hSK = word.word?.let {
                        (it.hskLevel?.takeIf{ lvl -> lvl != HSK_Level.NOT_HSK }  ?: "").toString()
                    } ?: ""
                    Text(
                        text = hSK,
                        modifier = Modifier.padding(start = 8.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                    )

                    onListsClick?.let {
                        RoundIconButton(
                            Res.drawable.format_list_bulleted_add_24px,
                            Res.string.dictionary_item_lists,
                            onListsClick
                        )
                    }
                }
            }

            if (isMoreVisible) {
                // More section
                Row {
                    Column(modifier = modifier.weight(1f)) {
                        val modality = word.word?.modality ?: Modality.UNKNOWN
                        if (modality != Modality.UNKNOWN) {
                            Text(
                                modality.toString().capitalize(),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        word.word?.synonyms?.takeIf { it.isNotEmpty() }?.let {
                            Text(stringResource(Res.string.dictionary_item_synonyms))
                            Text(it, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    Column(modifier = modifier.weight(1f), horizontalAlignment = Alignment.End) {
                        val wordType = word.word?.wordType ?: WordType.UNKNOWN
                        if (wordType != WordType.UNKNOWN) {
                            Text(
                                "Type: ${wordType.toString().capitalize()}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        word.word?.antonym?.takeIf { it.isNotEmpty() }?.let {
                            Text(stringResource(Res.string.dictionary_item_antonym))
                            Text(it, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                Row {
                    Column {
                        Text(stringResource(Res.string.dictionary_item_examples))

                        word.word?.examples?.takeIf { it.isNotEmpty() }?.let {
                            Text(it, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                Row {
                    Column {
                        Text(stringResource(Res.string.dictionary_item_altdefinition))

                        if (altDef.isNotEmpty()) {
                            Text(altDef, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}
