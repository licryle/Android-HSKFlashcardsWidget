package fr.berliat.hskwidget.ui.screens.wordlist

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding

import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import fr.berliat.hskwidget.data.model.WordList

import hskflashcardswidget.crossplatform.generated.resources.Res
import hskflashcardswidget.crossplatform.generated.resources.cancel
import hskflashcardswidget.crossplatform.generated.resources.wordlist_create_button
import hskflashcardswidget.crossplatform.generated.resources.wordlist_create_new_list_button
import hskflashcardswidget.crossplatform.generated.resources.wordlist_list_name
import hskflashcardswidget.crossplatform.generated.resources.wordlist_list_name_dupe
import hskflashcardswidget.crossplatform.generated.resources.wordlist_rename_button

import org.jetbrains.compose.resources.stringResource

@Composable
fun WordListCreateRenameDialog(
    viewModel: WordListViewModel,
    list: WordList? = null,
    onSuccess: () -> Unit,
    onCancel: () -> Unit,
    onError: () -> Unit = {},
    defaultName: String = "",
    modifier: Modifier = Modifier
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
                        color = Color.Red,
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
