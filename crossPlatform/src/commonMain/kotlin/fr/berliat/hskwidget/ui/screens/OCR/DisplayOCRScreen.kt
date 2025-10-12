package fr.berliat.hskwidget.ui.screens.OCR

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

import fr.berliat.hsktextviews.HSKTextSegmenter
import fr.berliat.hsktextviews.views.HSKTextView
import fr.berliat.hsktextviews.views.ShowPinyins
import fr.berliat.hskwidget.core.Utils

import fr.berliat.hskwidget.core.HSKAppServices
import fr.berliat.hskwidget.data.model.AnnotatedChineseWord
import fr.berliat.hskwidget.data.model.ChineseWord
import fr.berliat.hskwidget.data.store.AppPreferencesStore
import fr.berliat.hskwidget.ui.components.DetailedWordView
import fr.berliat.hskwidget.ui.components.Error
import fr.berliat.hskwidget.ui.components.ErrorView
import fr.berliat.hskwidget.ui.components.LoadingView
import fr.berliat.hskwidget.ui.screens.wordlist.WordListSelectionDialog

import fr.berliat.hskwidget.Res
import fr.berliat.hskwidget.ocr_display_add
import fr.berliat.hskwidget.ocr_display_conf_bigger
import fr.berliat.hskwidget.ocr_display_conf_smaller
import fr.berliat.hskwidget.ocr_display_loading
import fr.berliat.hskwidget.ocr_display_pinyins
import fr.berliat.hskwidget.ocr_display_separator
import fr.berliat.hskwidget.ocr_display_text_segmentation_failed
import fr.berliat.hskwidget.photo_camera_24px
import fr.berliat.hskwidget.text_decrease_24px
import fr.berliat.hskwidget.text_increase_24px
import fr.berliat.hskwidget.ui.components.PrettyCardShapeModifier
import fr.berliat.hskwidget.ui.theme.AppTypographies
import io.github.vinceglb.filekit.PlatformFile

import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun DisplayOCRScreen(
    modifier: Modifier = Modifier,
    viewModel: DisplayOCRViewModel = viewModel(factory = DisplayOCRViewModel.FACTORY),
    segmenter : HSKTextSegmenter = HSKAppServices.HSKSegmenter,
    appConfig : AppPreferencesStore = HSKAppServices.appPreferences,
    imageFile: PlatformFile? = null,
    preText: String = "",
    onClickOCRAdd : (String) -> Unit = {},
    onFavoriteClick : (AnnotatedChineseWord) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(preText) {
        viewModel.setText(preText)
    }

    LaunchedEffect(uiState.isSegmenterReady, imageFile) {
        if (uiState.isSegmenterReady && imageFile != null) {
            viewModel.recognizeText(imageFile)
        }
    }

    if (uiState.isSegmenterReady && imageFile == null && preText == "") {
        Utils.toast("Oops - nothing to display")
    }

    Column (modifier = modifier) {
        OcrDisplayConfig(modifier = modifier, viewModel = viewModel)

        Row(modifier = modifier.weight(1f)) {
            if (uiState.isProcessing) {
                LoadingView()
            } else {
                val error by viewModel.error.collectAsState()
                val err = error
                if (err != null) {
                    ErrorView(
                        modifier = modifier,
                        error = Error(err)
                    )
                } else {
                    Column {
                        HSKTextView(
                            text = uiState.text,
                            segmenter = segmenter,
                            hanziStyle = AppTypographies.hanzi.copy(fontSize = uiState.textSize.sp),
                            pinyinStyle = AppTypographies.pinyin,
                            clickedHanziStyle = AppTypographies.hanzi.copy(fontSize = uiState.textSize.sp),
                            clickedPinyinStyle = AppTypographies.pinyin,
                            clickedBackgroundColor = MaterialTheme.colorScheme.inverseOnSurface,
                            loadingComposable = { LoadingView(loadingText = Res.string.ocr_display_loading) },
                            emptyComposable = { OCRDisplayEmpty() },
                            onWordClick = { word -> viewModel.fetchWordForDisplay(word) },
                            showPinyins = if (uiState.showPinyins) ShowPinyins.ALL else ShowPinyins.CLICKED,
                            endSeparator = if (uiState.separatorEnabled) viewModel.wordSeparator else "",
                            clickedWords = uiState.clickedWords,
                            onTextAnalysisFailure = { e -> viewModel.setError(Res.string.ocr_display_text_segmentation_failed) }
                        )

                        OcrDisplayAdd(
                            modifier = modifier,
                            onClick = { onClickOCRAdd(uiState.text) }
                        )
                    }
                }
            }
        }

        var showWordListDialog by remember { mutableStateOf<ChineseWord?>(null) }
        showWordListDialog?.let { word ->
            WordListSelectionDialog(
                word = word,
                onDismiss = { showWordListDialog = null },
                onSaved = { showWordListDialog = null }
            )
        }

        uiState.selectedWord?.let { word ->
            DetailedWordView(
                word = word,
                showHSK3Definition = appConfig.dictionaryShowHSK3Definition.value,
                onFavoriteClick = onFavoriteClick,
                onSpeakClick = viewModel::speakWord,
                onCopyClick = viewModel::copyToClipboard,
                onListsClick = { showWordListDialog = word.word },
                shapeModifier = PrettyCardShapeModifier.First
            )
        }
    }
}

@Composable
private fun OCRDisplayEmpty() {
    Text("No text returned")
}

@Composable
private fun OcrDisplayConfig(
    viewModel: DisplayOCRViewModel,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Surface(
        modifier = modifier.fillMaxWidth(),
        tonalElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 5.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left switch + text
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f, fill = false)
            ) {
                Switch(
                    checked = uiState.separatorEnabled,
                    onCheckedChange = { viewModel.toggleSeparator(it) },
                    modifier = Modifier.padding(end = 5.dp)
                )
                Text(
                    text = stringResource(Res.string.ocr_display_separator),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            // Middle "font resize" control
            Surface(modifier = Modifier) {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    modifier = Modifier
                        .padding(horizontal = 10.dp)
                        .background(MaterialTheme.colorScheme.background.copy(alpha = 0.7f))
                        .padding(horizontal = 5.dp)
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.text_decrease_24px),
                        contentDescription = stringResource(Res.string.ocr_display_conf_smaller),
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier
                            .size(width = 35.dp, height = 30.dp)
                            .clickable { viewModel.updateTextSize(-2f) }
                            .padding(bottom = 2.6.dp)
                    )
                    Icon(
                        painter = painterResource(Res.drawable.text_increase_24px),
                        contentDescription = stringResource(Res.string.ocr_display_conf_bigger),
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier
                            .size(width = 45.dp, height = 40.dp)
                            .clickable { viewModel.updateTextSize(2f) }
                    )
                }
            }

            // Right text + switch
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f, fill = false),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = stringResource(Res.string.ocr_display_pinyins),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(end = 5.dp)
                )
                Switch(
                    checked = uiState.showPinyins,
                    onCheckedChange = { viewModel.toggleShowPinyins(it) }
                )
            }
        }
    }
}

@Composable
private fun OcrDisplayAdd(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(70.dp)
            .clickable { onClick() }, // makes whole area clickable
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(Res.drawable.photo_camera_24px),
            contentDescription = stringResource(Res.string.ocr_display_add),
            modifier = Modifier.size(50.dp),
            tint = MaterialTheme.colorScheme.onSurface // or Color.Unspecified if you want raw drawable
        )
        Text(
            text = stringResource(Res.string.ocr_display_add),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
    }
}
