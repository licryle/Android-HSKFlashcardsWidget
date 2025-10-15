package fr.berliat.hskwidget.ui.screens.widget

import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fr.berliat.hskwidget.core.HSKAppServices

import fr.berliat.hskwidget.ui.components.LoadingView
import fr.berliat.hskwidget.ui.components.WidgetEmptyWordView
import fr.berliat.hskwidget.ui.components.WidgetWordView

@Composable
fun WidgetView(
    widgetId: Int,
    modifier: Modifier = Modifier.border(
        width = 0.5.dp,
        color = MaterialTheme.colorScheme.primary,
        shape = RoundedCornerShape(20.dp))
) {
    // Initialize store + suspend singleton ViewModel
    val _viewModel by produceState<WidgetViewModel?>(initialValue = null, widgetId) {
        val store = HSKAppServices.widgetsPreferencesProvider(widgetId) // suspend
        value = WidgetViewModel(store, HSKAppServices.database) // suspend
    }

    val viewModel = _viewModel
    if (viewModel == null) {
        LoadingView(modifier = modifier)
    } else {
        val wordHandle = viewModel.word.collectAsState()

        val word = wordHandle.value
        if (word == null) {
            WidgetEmptyWordView(modifier = modifier)
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
