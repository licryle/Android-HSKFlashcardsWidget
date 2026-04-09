package fr.berliat.hskwidget.ui.screens.widget

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

import fr.berliat.hskwidget.core.Locale
import fr.berliat.hskwidget.data.model.ChineseWord
import fr.berliat.hskwidget.data.type.HSK_Level
import fr.berliat.hskwidget.data.type.Pinyins
import fr.berliat.hskwidget.ui.components.IconButton
import fr.berliat.hskwidget.ui.components.WidgetWordView
import fr.berliat.hskwidget.ui.screens.widgetConfigure.WidgetConfigWithPreviewScreen
import fr.berliat.hskwidget.*
import fr.berliat.hskwidget.core.HSKAppServices
import fr.berliat.hskwidget.core.SnackbarType
import fr.berliat.hskwidget.data.model.AnnotatedChineseWord
import fr.berliat.hskwidget.ui.theme.widgetDefaultBox

import kotlinx.coroutines.launch

import org.jetbrains.compose.resources.stringResource

import kotlin.math.max

@Composable
actual fun WidgetsListScreen(
    onWidgetPreferenceSaved: (Int) -> Unit,
    expectsActivityResult: Boolean,
    modifier: Modifier,
    selectedWidgetId: Int?,
    viewModel: WidgetsListViewModel
) {
    val widgetIds by viewModel.widgetIds.collectAsState()
    val showAddWidgetInstructions by viewModel.showAddWidgetInstructions.collectAsState()
    val scope = rememberCoroutineScope()

    if (showAddWidgetInstructions) {
        AlertDialog(
            onDismissRequest = viewModel::dismissAddWidgetInstructions,
            title = { Text(text = stringResource(Res.string.widgets_add_widget)) },
            text = { Text(text = stringResource(Res.string.widgets_add_widget_instructions)) },
            confirmButton = {
                TextButton(onClick = viewModel::dismissAddWidgetInstructions) {
                    Text(text = stringResource(Res.string.understood))
                }
            }
        )
    }

    val ids = widgetIds
    Column(modifier = modifier.fillMaxSize()) {
        if (ids.isEmpty()) {
            // Intro + demo flashcard
            Text(
                text = stringResource(Res.string.widgets_intro),
                modifier = Modifier.padding(16.dp),
                textAlign = TextAlign.Start
            )

            Text(
                text = stringResource(Res.string.widgets_intro2),
                modifier = Modifier.padding(16.dp),
                textAlign = TextAlign.Start
            )

            Box(
                modifier = widgetDefaultBox.align(Alignment.CenterHorizontally)
            ) {
                val placeholderWord = stringResource(Res.string.widget_placeholder_word)
                val placeholderPinyin = stringResource(Res.string.widget_placeholder_pinyin)
                val placeholderLanguage = stringResource(Res.string.widget_placeholder_language)
                val placeholderHSKLevel = stringResource(Res.string.widget_placeholder_level)
                val placeholderDefinition = stringResource(Res.string.widget_placeholder_definition)

                WidgetWordView(
                    AnnotatedChineseWord(
                        word = ChineseWord(
                            simplified = placeholderWord,
                            definition = mapOf((Locale.fromCode(placeholderLanguage) ?: Locale.getDefault()) to placeholderDefinition),
                            hskLevel = HSK_Level.valueOf(placeholderHSKLevel),
                            pinyins = Pinyins.fromString(placeholderPinyin),
                            traditional = placeholderWord,
                            popularity = null
                        ),
                        annotation = null,
                    ),
                    onClickWord = { HSKAppServices.snackbar.show(SnackbarType.INFO, Res.string.widget_demo_word_click) },
                    onClickSpeak = { viewModel.speakWord(placeholderWord) },
                    onClickUpdate = { HSKAppServices.snackbar.show(SnackbarType.INFO, Res.string.widget_demo_update_click) }
                )
            }
        }

        IconButton(
            text = stringResource(Res.string.widgets_add_widget),
            onClick = viewModel::addNewWidget,
            drawable = Res.drawable.ic_add_24dp,
            modifier = Modifier.fillMaxWidth()
        )

        if (ids.isNotEmpty()) {
            val pagerState = rememberPagerState(
                initialPage = max(ids.indexOf(selectedWidgetId), 0),
                pageCount = { ids.size }
            )

            LaunchedEffect(ids, selectedWidgetId) { // Updates page if either changes
                val targetPage = max(ids.indexOf(selectedWidgetId), 0).coerceAtMost(ids.size - 1)
                if (targetPage != pagerState.currentPage) {
                    pagerState.scrollToPage(targetPage)
                }
            }

            // Tabs
            TabRow(selectedTabIndex = pagerState.currentPage) {
                ids.forEachIndexed { index, _ ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = {
                            scope.launch { pagerState.animateScrollToPage(index) }
                        },
                        text = { Text("Widget $index") }
                    )
                }
            }

            // Pager for widgets
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                WidgetConfigWithPreviewScreen(
                    widgetId = ids[page],
                    expectsActivityResult = expectsActivityResult,
                    onSuccessfulSave = {
                        HSKAppServices.snackbar.show(SnackbarType.SUCCESS, Res.string.widget_configure_saved)
                        onWidgetPreferenceSaved(ids[page])
                    },
                    modifier = modifier
                )
            }
        }
    }
}
