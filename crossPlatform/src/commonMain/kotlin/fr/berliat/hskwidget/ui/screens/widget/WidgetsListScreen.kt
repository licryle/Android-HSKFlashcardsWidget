package fr.berliat.hskwidget.ui.screens.widget

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
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
import fr.berliat.hskwidget.ui.components.widgetDefaultBox
import fr.berliat.hskwidget.ui.screens.widgetConfigure.WidgetConfigWithPreviewScreen

import hskflashcardswidget.crossplatform.generated.resources.Res
import hskflashcardswidget.crossplatform.generated.resources.ic_add_24dp
import hskflashcardswidget.crossplatform.generated.resources.widget_configure_saved
import hskflashcardswidget.crossplatform.generated.resources.widgets_add_widget
import hskflashcardswidget.crossplatform.generated.resources.widgets_intro

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
    val pagerState = rememberPagerState(
        initialPage = max(widgetIds.value.indexOf(selectedWidgetId), 0),
        pageCount = { widgetIds.value.size }
    )

    LaunchedEffect(widgetIds, selectedWidgetId) { // Updates page if either changes
        val targetPage = max(widgetIds.value.indexOf(selectedWidgetId), 0)
        if (targetPage != pagerState.currentPage) {
            pagerState.scrollToPage(targetPage)
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        if (widgetIds.value.isEmpty()) {
            // Intro + demo flashcard
            Text(
                text = stringResource(Res.string.widgets_intro),
                modifier = Modifier.padding(16.dp),
                textAlign = TextAlign.Start
            )

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
                    )
                )
            }
        }

        IconButton(
            text = stringResource(Res.string.widgets_add_widget),
            onClick = viewModel::addNewWidget,
            drawable = Res.drawable.ic_add_24dp
        )

        if (!widgetIds.value.isEmpty()) {
            // Tabs
            TabRow(selectedTabIndex = pagerState.currentPage) {
                widgetIds.value.forEachIndexed { index, _ ->
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
                    widgetId = widgetIds.value[page],
                    expectsActivityResult = expectsActivityResult,
                    onSuccessfulSave = {
                        Utils.toast(Res.string.widget_configure_saved)
                        onWidgetPreferenceSaved(widgetIds.value[page])
                    },
                    modifier = modifier
                )
            }
        }
    }
}
