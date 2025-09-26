package fr.berliat.hskwidget.ui.screens.widget

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import fr.berliat.hskwidget.core.HSKAppServices

import fr.berliat.hskwidget.ui.components.LoadingView
import fr.berliat.hskwidget.ui.components.WidgetWordView

@Composable
fun WidgetView(
    widgetId: Int,
    modifier: Modifier = Modifier,
) {
    // Initialize store + suspend singleton ViewModel
    val _viewModel by produceState<WidgetViewModel?>(initialValue = null, widgetId) {
        val store = HSKAppServices.widgetsPreferencesProvider(widgetId) // suspend
        value = WidgetViewModel.getInstance(store, HSKAppServices.database) // suspend
    }

    val viewModel = _viewModel
    if (viewModel == null) {
        LoadingView()
    } else {
        val wordHandle = viewModel.word.collectAsState()

        val word = wordHandle.value
        if (word == null) {
            LoadingView()
        } else {
            WidgetWordView(
                word = word,
                onClickUpdate = viewModel::updateWord,
                onClickSpeak = viewModel::speakWord,
                onClickWord = viewModel::openDictionary,
                modifier = modifier
            )
        }
    }
}
