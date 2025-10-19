package fr.berliat.hskwidget.ui.screens.widget

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

import fr.berliat.hskwidget.core.Utils
import fr.berliat.hskwidget.core.Locale
import fr.berliat.hskwidget.data.model.ChineseWord
import fr.berliat.hskwidget.data.type.HSK_Level
import fr.berliat.hskwidget.data.type.Pinyins
import fr.berliat.hskwidget.ui.components.IconButton
import fr.berliat.hskwidget.ui.components.WidgetWordView
import fr.berliat.hskwidget.ui.screens.widgetConfigure.WidgetConfigWithPreviewScreen
import fr.berliat.hskwidget.Res
import fr.berliat.hskwidget.ic_add_24dp
import fr.berliat.hskwidget.ui.theme.widgetDefaultBox
import fr.berliat.hskwidget.widget_configure_saved
import fr.berliat.hskwidget.widget_demo_update_click
import fr.berliat.hskwidget.widget_demo_word_click
import fr.berliat.hskwidget.widgets_add_widget
import fr.berliat.hskwidget.widgets_intro

import kotlinx.coroutines.launch

import org.jetbrains.compose.resources.stringResource

import kotlin.math.max

@Composable
fun WidgetsListScreen(
    onWidgetPreferenceSaved: (Int) -> Unit,
    expectsActivityResult: Boolean,
    modifier: Modifier = Modifier,
    selectedWidgetId: Int? = null,
    viewModel: WidgetsListViewModel = WidgetsListViewModel()
) {
    val widgetIds = viewModel.widgetIds.collectAsState()
    val scope = rememberCoroutineScope()

    val ids = widgetIds.value
    Column(modifier = modifier.fillMaxSize()) {
        if (ids.isEmpty()) {
            // Intro + demo flashcard
            Text(
                text = stringResource(Res.string.widgets_intro),
                modifier = Modifier.padding(16.dp),
                textAlign = TextAlign.Start
            )

            val onClickWordToast = stringResource(Res.string.widget_demo_word_click)
            val onClickUpdateToast = stringResource(Res.string.widget_demo_update_click)

            Box(
                modifier = widgetDefaultBox.align(Alignment.CenterHorizontally)
            ) {
                WidgetWordView(
                    ChineseWord(
                        simplified = "你好",
                        definition = mapOf(Locale.ENGLISH to "Hello"),
                        hskLevel = HSK_Level.HSK1,
                        pinyins = Pinyins.fromString("nǐ hǎo"),
                        traditional = "你好",
                        popularity = null
                    ),
                    onClickWord = { viewModel.toast(onClickWordToast) },
                    onClickSpeak = { viewModel.speakWord("你好") },
                    onClickUpdate = { viewModel.toast(onClickUpdateToast) }
                )
            }
        }

        IconButton(
            text = stringResource(Res.string.widgets_add_widget),
            onClick = viewModel::addNewWidget,
            drawable = Res.drawable.ic_add_24dp,
            modifier = Modifier.fillMaxWidth()
        )

        if (!ids.isEmpty()) {
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
                        Utils.toast(Res.string.widget_configure_saved)
                        onWidgetPreferenceSaved(ids[page])
                    },
                    modifier = modifier
                )
            }
        }
    }
}
