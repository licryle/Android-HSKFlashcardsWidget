package fr.berliat.hskwidget.ui.application.content

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager

import fr.berliat.hskwidget.Res
import fr.berliat.hskwidget.close_24px
import fr.berliat.hskwidget.menu_24px
import fr.berliat.hskwidget.menu_ocr
import fr.berliat.hskwidget.photo_camera_24px
import fr.berliat.hskwidget.search_hint

import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppBar(
    title: String,
    onOcrClick: () -> Unit,
    onSearch: (String) -> Unit,
    onMenuClick: () -> Unit,
    viewModel: AppBarViewModel = remember { AppBarViewModel() }
) {
    val searchQuery = viewModel.searchQuery.collectAsState("")
    val localText = remember { mutableStateOf(searchQuery.value.toString()) }

    // Update localText only if different from current user input
    LaunchedEffect(searchQuery.value) {
        if (localText.value != searchQuery.value.toString()) {
            localText.value = searchQuery.value.toString()
        }
    }

    fun onValueChange(newValue: String) {
        localText.value = newValue
        onSearch(newValue)
    }

    TopAppBar(
        title = {
            Row {
                Text(title)

                val focusRequester = remember { FocusRequester() }
                val focusManager = LocalFocusManager.current
                TextField(
                    value = localText.value,
                    onValueChange = { onValueChange(it) },
                    placeholder = { Text(stringResource(Res.string.search_hint)) },
                    modifier = Modifier.fillMaxWidth()
                        .focusRequester(focusRequester),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                        }
                    ),
                    singleLine = true,
                    trailingIcon = {
                        if (localText.value.isNotEmpty()) {
                            IconButton(onClick = {
                                focusRequester.requestFocus()
                                onValueChange("")
                            }) {
                                Icon(painterResource(Res.drawable.close_24px), contentDescription = "Clear text")
                            }
                        }
                    }
                )
            }
        },
        navigationIcon = {
            IconButton(onClick = onMenuClick) {
                Icon(painterResource(Res.drawable.menu_24px), contentDescription = "Menu")
            }
        },
        actions = {
            IconButton(onClick = onOcrClick) {
                Icon(
                    painterResource(Res.drawable.photo_camera_24px),
                    contentDescription = stringResource(Res.string.menu_ocr)
                )
            }
        }
    )
}
