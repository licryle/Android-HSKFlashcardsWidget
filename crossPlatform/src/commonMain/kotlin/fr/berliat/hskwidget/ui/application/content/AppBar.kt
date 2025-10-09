package fr.berliat.hskwidget.ui.application.content

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp

import fr.berliat.hskwidget.Res
import fr.berliat.hskwidget.close_24px
import fr.berliat.hskwidget.menu
import fr.berliat.hskwidget.menu_24px
import fr.berliat.hskwidget.menu_ocr
import fr.berliat.hskwidget.photo_camera_24px
import fr.berliat.hskwidget.search_24px
import fr.berliat.hskwidget.search_clear
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
            Row(verticalAlignment = Alignment.CenterVertically, // center children vertically
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.animateContentSize()
            ) {
                val focusRequester = remember { FocusRequester() }
                val focusManager = LocalFocusManager.current
                var isSearchFocused by remember { mutableStateOf(false) }

                if (!isSearchFocused) {
                    Text(title, modifier = Modifier.padding(end = 8.dp))

                    Spacer(modifier = Modifier.weight(1f))
                }

                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                    if (!isSearchFocused) {
                        IconButton(onClick = { focusRequester.requestFocus(); isSearchFocused = true; }) {
                            Icon(
                                painterResource(Res.drawable.search_24px),
                                contentDescription = stringResource(Res.string.search_hint)
                            )
                        }
                    } else {
                        LaunchedEffect(Unit) {
                            focusRequester.requestFocus()
                        }

                        TextField(
                            value = localText.value,
                            onValueChange = { onValueChange(it) },
                            placeholder = { Text(stringResource(Res.string.search_hint)) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester)
                                .onFocusChanged { focusState ->
                                    isSearchFocused = focusState.isFocused
                                },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = {
                                focusManager.clearFocus()
                            }),
                            singleLine = true,
                            trailingIcon = {
                                if (localText.value.isNotEmpty()) {
                                    IconButton(onClick = {
                                        focusRequester.requestFocus()
                                        onValueChange("")
                                    }) {
                                        Icon(
                                            painterResource(Res.drawable.close_24px),
                                            contentDescription = stringResource(Res.string.search_clear)
                                        )
                                    }
                                }
                            }
                        )
                    }
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = onMenuClick) {
                Icon(painterResource(Res.drawable.menu_24px),
                    contentDescription = stringResource(Res.string.menu))
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
