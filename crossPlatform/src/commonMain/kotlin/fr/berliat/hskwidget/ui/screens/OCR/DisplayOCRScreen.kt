package fr.berliat.hskwidget.ui.screens.OCR

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

        val word = uiState.selectedWord?.simplified ?: ""
        if (word != "")
            viewModel.fetchWordForDisplay(word)
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
                pinyinEditable = false,
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
    val scrollState = rememberScrollState()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(start = 10.dp, end = 10.dp, top = 0.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OcrTextSizeChip(
            onDecrease = { viewModel.updateTextSize(-2f) },
            onIncrease = { viewModel.updateTextSize(+2f) },
            modifier = Modifier.padding(end = 8.dp)
        )

        FilterChip(
            selected = uiState.separatorEnabled,
            onClick = { viewModel.toggleSeparator(!uiState.separatorEnabled) },
            shape = RoundedCornerShape(50),
            modifier = Modifier.padding(end = 8.dp),
            label = {
                Text(
                    text = stringResource(Res.string.ocr_display_separator),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        )

        FilterChip(
            selected = uiState.showPinyins,
            onClick = { viewModel.toggleShowPinyins(!uiState.showPinyins) },
            shape = RoundedCornerShape(50),
            modifier = Modifier.padding(end = 8.dp),
            label = {
                Text(
                    text = stringResource(Res.string.ocr_display_pinyins),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        )
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

@Composable
private fun OcrTextSizeChip(
    modifier: Modifier = Modifier,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
) {
    val borderWidth = 0.6.dp
    val horizontalPadding = 13.dp
    val halfPillWidth = 60.dp
    val pillHeight = 32.dp

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        tonalElevation = 2.dp,
        color = MaterialTheme.colorScheme.background,
        border = BorderStroke(borderWidth, MaterialTheme.colorScheme.onSurfaceVariant),
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .height(pillHeight)
                .wrapContentWidth()
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(topStart = 50.dp, bottomStart = 50.dp))
                    .clickable { onDecrease() }
                    .fillMaxHeight()
                    .width(halfPillWidth)
                    .padding(horizontal = horizontalPadding, vertical = 5.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Icon(
                    painter = painterResource(Res.drawable.text_decrease_24px),
                    contentDescription = stringResource(Res.string.ocr_display_conf_smaller),
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            VerticalDivider(thickness = borderWidth, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(topEnd = 50.dp, bottomEnd = 50.dp))
                    .clickable { onIncrease() }
                    .fillMaxHeight()
                    .width(halfPillWidth)
                    .padding(horizontal = horizontalPadding, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(Res.drawable.text_increase_24px),
                    contentDescription = stringResource(Res.string.ocr_display_conf_bigger),
                    modifier = Modifier.size(30.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}