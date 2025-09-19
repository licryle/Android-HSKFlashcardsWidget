package fr.berliat.hskwidget.ui.screens.wordlist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import fr.berliat.hskwidget.AnkiDelegator

import fr.berliat.hskwidget.data.model.WordList
import fr.berliat.hskwidget.data.model.WordListWithCount
import fr.berliat.hskwidget.ui.components.ConfirmDeletionDialog
import fr.berliat.hskwidget.YYMMDD
import fr.berliat.hskwidget.ui.components.LoadingView
import fr.berliat.hskwidget.ui.components.PrettyCard
import fr.berliat.hskwidget.ui.components.RoundIconButton

import hskflashcardswidget.crossplatform.generated.resources.Res
import hskflashcardswidget.crossplatform.generated.resources.annotation_edit_delete_confirm_message
import hskflashcardswidget.crossplatform.generated.resources.annotation_edit_delete_confirm_title
import hskflashcardswidget.crossplatform.generated.resources.delete_24px
import hskflashcardswidget.crossplatform.generated.resources.edit_24px
import hskflashcardswidget.crossplatform.generated.resources.ic_add_24dp
import hskflashcardswidget.crossplatform.generated.resources.wordlist_create_new_list_button
import hskflashcardswidget.crossplatform.generated.resources.wordlist_createddate
import hskflashcardswidget.crossplatform.generated.resources.wordlist_delete_button
import hskflashcardswidget.crossplatform.generated.resources.wordlist_editeddate
import hskflashcardswidget.crossplatform.generated.resources.wordlist_rename_button
import hskflashcardswidget.crossplatform.generated.resources.wordlist_word_count

import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun WordListScreen(
    ankiCaller : AnkiDelegator,
    onClickList: (WordList) -> Unit
) {
    val viewModel = rememberSuspendWordListViewModel(ankiCaller)
    if (viewModel == null) {
        LoadingView()
        return
    }

    val wordLists by viewModel.allLists.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }
    var confirmDeleteList by remember { mutableStateOf<WordList?>(null) }
    var wordListToRename by remember { mutableStateOf<WordList?>(null) }

    if (showCreateDialog) {
        WordListCreateRenameDialog(
            viewModel = viewModel,
            list = null,
            onSuccess = { showCreateDialog = false },
            onCancel = { showCreateDialog = false }
        )
    }

    val cDL = confirmDeleteList
    if (cDL != null) {
        ConfirmDeletionDialog(
            title = Res.string.annotation_edit_delete_confirm_title,
            message = Res.string.annotation_edit_delete_confirm_message,
            onConfirm = {
                viewModel.deleteList(cDL)
                confirmDeleteList = null
            },
            onDismiss = { confirmDeleteList = null }
        )
    }

    val wLTR = wordListToRename
    wLTR?.let {
        WordListCreateRenameDialog(
            viewModel = viewModel,
            list = wordListToRename,
            onSuccess = { wordListToRename = null },
            onCancel = { wordListToRename = null },
            defaultName = wLTR.name
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(8.dp)) {
            itemsIndexed(wordLists) { index, wordList ->
                WordListRow(
                    wordList = wordList,
                    onClick = { onClickList(wordList.wordList) },
                    onRename = { wordListToRename = wordList.wordList },
                    onDelete = { confirmDeleteList = wordList.wordList }
                )
            }
        }

        FloatingActionButton(
            onClick = { showCreateDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            content = {
                Icon(
                    painter = painterResource(Res.drawable.ic_add_24dp),
                    contentDescription = stringResource(Res.string.wordlist_create_new_list_button)
                )
            }
        )
    }
}

@Composable
private fun WordListRow(
    wordList: WordListWithCount,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    PrettyCard(
        onClick = onClick
    ) {
        Row(modifier = Modifier, verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.SpaceBetween) {

            val hideBtn = (wordList.listType == WordList.ListType.SYSTEM)
            RoundIconButton(
                iconRes = Res.drawable.edit_24px,
                contentDescriptionRes = Res.string.wordlist_rename_button,
                onClick = { if (!hideBtn) onRename() },
                modifier = Modifier.alpha(if (hideBtn) 0f else 1f)
            )

            Column(modifier = Modifier
                .weight(1f)
                .padding(start = 10.dp)) {
                Text(
                    text = wordList.name,
                    style = MaterialTheme.typography.titleMedium
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = stringResource(Res.string.wordlist_word_count, wordList.wordCount),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Column {
                        Text(
                            text = stringResource(Res.string.wordlist_editeddate, wordList.lastModified.YYMMDD()),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = stringResource(Res.string.wordlist_createddate, wordList.creationDate.YYMMDD()),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            RoundIconButton(
                iconRes = Res.drawable.delete_24px,
                contentDescriptionRes = Res.string.wordlist_delete_button,
                onClick = { if (!hideBtn) onDelete() },
                modifier = Modifier.alpha(if (hideBtn) 0f else 1f)
            )
        }
    }
}
