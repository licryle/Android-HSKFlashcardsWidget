package fr.berliat.hskwidget.ui.screens.widgetConfigure

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import fr.berliat.hskwidget.data.model.WordListWithCount
import fr.berliat.hskwidget.ui.components.LoadingView
import fr.berliat.hskwidget.ui.components.widgetDefaultBox
import fr.berliat.hskwidget.ui.screens.widget.WidgetView

import fr.berliat.hskwidget.Res
import fr.berliat.hskwidget.ic_dictionary_24dp
import fr.berliat.hskwidget.widget_configure
import fr.berliat.hskwidget.widget_configure_close
import fr.berliat.hskwidget.widget_configure_error_no_list
import fr.berliat.hskwidget.widget_configure_new_back
import fr.berliat.hskwidget.widget_configure_no_change
import fr.berliat.hskwidget.widget_configure_wordlist_title
import fr.berliat.hskwidget.widget_configure_wordlist_word_count

import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun WidgetConfigWithPreviewScreen(
    widgetId: Int,
    onSuccessfulSave : () -> Unit,
    modifier: Modifier = Modifier,
    expectsActivityResult: Boolean = false,
    viewModel: WidgetConfigViewModel = remember(widgetId) {
        WidgetConfigViewModel(widgetId, onSuccessfulSave = onSuccessfulSave)
    }
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(top = 15.dp)
    ) {
        Box(widgetDefaultBox.align(Alignment.CenterHorizontally)) {
            WidgetView(widgetId = widgetId, modifier = modifier)
        }

        Spacer(modifier = modifier.height(10.dp))

        WidgetConfigScreen(
            widgetId = widgetId,
            expectsActivityResult = expectsActivityResult,
            onSuccessfulSave = onSuccessfulSave,
            viewModel = viewModel,
            modifier = modifier
        )
    }
}

@Composable
fun WidgetConfigScreen(
    widgetId: Int,
    onSuccessfulSave : () -> Unit,
    modifier: Modifier = Modifier,
    expectsActivityResult: Boolean = false,
    viewModel: WidgetConfigViewModel = remember(widgetId) {
        WidgetConfigViewModel(widgetId, onSuccessfulSave = onSuccessfulSave)
    }
) {
    val allLists = viewModel.allLists.collectAsState()
    val selectedIds = viewModel.selectedListIds.collectAsState()
    val localSelectedIds = remember(selectedIds) { mutableStateSetOf<Long>() }

    LaunchedEffect(selectedIds) {
        // Clear and update the local state only when the ViewModel state changes
        localSelectedIds.clear()
        localSelectedIds.addAll(selectedIds.value)
    }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(start = 10.dp, top = 5.dp, bottom = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(Res.drawable.ic_dictionary_24dp),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = stringResource(Res.string.widget_configure_wordlist_title),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
            )
        }

        if (allLists.value.isEmpty()) {
            LoadingView()
        } else {
            // List container (scrollable)
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp)
            ) {
                items(allLists.value) { list ->
                    FlashcardConfigListItem(
                        list = list,
                        isSelected = localSelectedIds.contains(list.id),
                        onToggle = { included ->
                            if (included) localSelectedIds.add(list.id) else localSelectedIds.remove(
                                list.id
                            )
                        }
                    )
                }
            }

            val configureButtonLabel = when {
                localSelectedIds.isEmpty() -> Res.string.widget_configure_error_no_list
                localSelectedIds.toSet() == selectedIds.value.toSet() -> Res.string.widget_configure_no_change
                expectsActivityResult && selectedIds.value.isEmpty() -> Res.string.widget_configure_new_back
                expectsActivityResult -> Res.string.widget_configure_close
                else -> Res.string.widget_configure
            }

            // Confirm button
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                onClick = { viewModel.savePreferences(localSelectedIds) },
                enabled = !localSelectedIds.isEmpty() && localSelectedIds.toSet() != selectedIds.value.toSet()
            ) {
                Text(text = stringResource(configureButtonLabel))
            }
        }
    }
}

// Todo: solve that flicker
@Composable
private fun FlashcardConfigListItem(
    list: WordListWithCount,
    isSelected: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = list.name,
            modifier = Modifier.weight(1f),
            fontSize = 17.sp,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = stringResource(Res.string.widget_configure_wordlist_word_count, list.wordCount),
            fontSize = 17.sp,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(end = 8.dp)
        )
        Switch(
            checked = isSelected,
            onCheckedChange = onToggle
        )
    }
}
