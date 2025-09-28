package fr.berliat.hskwidget.ui.screens.wordlist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

import fr.berliat.hskwidget.KAnkiDelegator
import fr.berliat.hskwidget.core.HSKAppServices
import fr.berliat.hskwidget.data.model.ChineseWord
import fr.berliat.hskwidget.data.model.WordListWithCount
import fr.berliat.hskwidget.ui.components.LoadingView

import hskflashcardswidget.crossplatform.generated.resources.Res
import hskflashcardswidget.crossplatform.generated.resources.cancel
import hskflashcardswidget.crossplatform.generated.resources.save
import hskflashcardswidget.crossplatform.generated.resources.wordlist_create_button
import hskflashcardswidget.crossplatform.generated.resources.wordlist_select_lists
import hskflashcardswidget.crossplatform.generated.resources.wordlist_word_count

import org.jetbrains.compose.resources.stringResource

@Composable
fun WordListSelectionDialog(
    ankiCaller: KAnkiDelegator,
    word: ChineseWord,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel = remember(ankiCaller) { WordListViewModel(HSKAppServices.wordListRepo, ankiCaller) }

    val allLists by viewModel.userLists.collectAsState()
    val shouldDismiss by viewModel.dismiss.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }

    val selectedWordListIds = remember { mutableStateOf(setOf<Long>()) }
    var status by remember { mutableStateOf(WordListViewModel.Status.STARTING) }

    if (shouldDismiss) onDismiss()


    LaunchedEffect(viewModel) {
        viewModel.status.collect { st ->
            status = st

            if (st == WordListViewModel.Status.SUCCESS) { onSaved() }
        }
    }

    LaunchedEffect(word) {
        selectedWordListIds.value = viewModel.getWordListsForWord(word)
    }

    if (showCreateDialog) {
        WordListCreateRenameDialog(
            viewModel = viewModel,
            onSuccess = { showCreateDialog = false },
            onCancel = { showCreateDialog = false }
        )
    }

    Dialog(
        onDismissRequest = onDismiss
    ) {
        // Apply the Material surface & shape
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            shadowElevation = 6.dp,
            modifier = Modifier.padding(10.dp)
                .wrapContentSize()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column {
                    Text(
                        stringResource(Res.string.wordlist_select_lists),
                        style = MaterialTheme.typography.titleLarge
                    )

                    if (status == WordListViewModel.Status.STARTING) {
                        LoadingView()
                        return@Column
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                    ) {
                        itemsIndexed(allLists) { index, wordList ->
                            WordListCheckRow(
                                wordList = wordList,
                                initialSelected = selectedWordListIds.value.contains(wordList.id),
                                onSelectionToggle = { isSelected: Boolean ->
                                    selectedWordListIds.value = if (isSelected) {
                                        selectedWordListIds.value.plus(wordList.id)
                                    } else {
                                        selectedWordListIds.value.minus(wordList.id)
                                    }
                                }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = modifier.weight(1f)
                        ) {
                            Text(text = stringResource(Res.string.cancel))
                        }
                        OutlinedButton(
                            onClick = { showCreateDialog = true },
                            modifier = modifier.weight(1f)
                        ) {
                            Text(text = stringResource(Res.string.wordlist_create_button))
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 0.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.saveAssociations(
                                    word,
                                    selectedWordListIds.value
                                )
                            },
                            enabled = status != WordListViewModel.Status.SAVING,
                            modifier = modifier.weight(1f)
                        ) {
                            Text(text = stringResource(Res.string.save))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WordListCheckRow(
    wordList: WordListWithCount,
    initialSelected: Boolean,
    onSelectionToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var selected by remember { mutableStateOf(initialSelected) }

    LaunchedEffect(selected) {
        onSelectionToggle(selected)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { selected = !selected }
            .padding(0.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = selected,
            onCheckedChange = { selected = !selected }
        )
        Column(modifier = Modifier.padding(start = 8.dp)) {
            Text(
                text = wordList.name,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = stringResource(
                    Res.string.wordlist_word_count,
                    wordList.wordCount
                ),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
