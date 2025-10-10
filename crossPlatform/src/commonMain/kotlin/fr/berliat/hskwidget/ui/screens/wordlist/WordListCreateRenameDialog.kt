package fr.berliat.hskwidget.ui.screens.wordlist

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fr.berliat.hskwidget.core.HSKAppServices
import fr.berliat.hskwidget.data.model.WordList

import fr.berliat.hskwidget.Res
import fr.berliat.hskwidget.cancel
import fr.berliat.hskwidget.wordlist_create_button
import fr.berliat.hskwidget.wordlist_create_new_list_button
import fr.berliat.hskwidget.wordlist_list_name
import fr.berliat.hskwidget.wordlist_list_name_dupe
import fr.berliat.hskwidget.wordlist_rename_button

import org.jetbrains.compose.resources.stringResource

@Composable
fun WordListCreateRenameDialog(
    list: WordList? = null,
    onSuccess: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    onError: () -> Unit = {},
    defaultName: String = "",
    viewModel: WordListViewModel = remember {
        WordListViewModel(HSKAppServices.wordListRepo, HSKAppServices.ankiDelegator)
    }
) {
    var listName by remember { mutableStateOf(defaultName) }
    val error = remember { mutableStateOf<String?>(null) }
    val errorStr = stringResource(Res.string.wordlist_list_name_dupe)

    LaunchedEffect(viewModel) {
        viewModel.status.collect { status ->
            when (status) {
                WordListViewModel.Status.SUCCESS -> onSuccess()
                WordListViewModel.Status.ERROR -> {
                    error.value = errorStr
                    onError()
                }
                else -> {}
            }
        }
    }


    val titleRes = if (list == null) {
        Res.string.wordlist_create_new_list_button
    } else {
        Res.string.wordlist_rename_button
    }

    AlertDialog(
        modifier = modifier,
        onDismissRequest = onCancel,
        title = { Text(text = stringResource(titleRes)) },
        text = {
            Column {
                TextField(
                    value = listName,
                    onValueChange = { listName = it },
                    label = { Text(stringResource(Res.string.wordlist_list_name)) }
                )
                error.value?.let {
                    Text(
                        text = "Error: $it",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                error.value = null
                if (list == null) {
                    viewModel.createList(listName)
                } else {
                    viewModel.renameList(list, listName)
                }
            }) {
                Text(stringResource(Res.string.wordlist_create_button))
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text(stringResource(Res.string.cancel))
            }
        }
    )
}
