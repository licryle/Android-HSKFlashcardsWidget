package fr.berliat.hskwidget.ui.screens.widget

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
expect fun WidgetsListScreen(
    onWidgetPreferenceSaved: (Int) -> Unit,
    expectsActivityResult: Boolean,
    modifier: Modifier = Modifier,
    selectedWidgetId: Int? = null,
    viewModel: WidgetsListViewModel = viewModel { WidgetsListViewModel() }
)
