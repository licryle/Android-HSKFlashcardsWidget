package fr.berliat.hskwidget.ui.application.content

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

import fr.berliat.hskwidget.Res
import fr.berliat.hskwidget.menu
import fr.berliat.hskwidget.menu_24px
import fr.berliat.hskwidget.menu_ocr
import fr.berliat.hskwidget.photo_camera_24px
import fr.berliat.hskwidget.search_hint

import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppBar(
    onOcrClick: () -> Unit,
    onSearch: (String) -> Unit,
    onMenuClick: () -> Unit,
    viewModel: AppBarViewModel = remember { AppBarViewModel() }
) {
    val searchQuery = viewModel.searchQuery.collectAsState()
    var localText by remember { mutableStateOf(TextFieldValue(searchQuery.value.toString())) }
    var isSearchFocused by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    var debounceJob by remember { mutableStateOf<Job?>(null) }

    // Update localText only if different from current user input
    LaunchedEffect(searchQuery.value) {
        if (!isSearchFocused && localText.text != searchQuery.value.toString()) {
            localText = localText.copy(searchQuery.value.toString())
        }
    }

    fun onValueChange(newValue: TextFieldValue) {
        localText = newValue
        debounceJob?.cancel()
        debounceJob = coroutineScope.launch {
            delay(300) // 300ms debounce
            if (localText.text != searchQuery.value.toString()) {
                onSearch(localText.text)
            }
        }
    }

    TopAppBar(
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            titleContentColor = MaterialTheme.colorScheme.onBackground,
            navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
            actionIconContentColor = MaterialTheme.colorScheme.onBackground
        ),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, // center children vertically
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                val focusRequester = remember { FocusRequester() }

                LaunchedEffect(Unit) {
                    focusRequester.requestFocus()
                }

                PillSearchBar(
                    query = localText,
                    onQueryChange = { onValueChange(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .onFocusChanged { focusState ->
                            isSearchFocused = focusState.isFocused

                            localText = localText.copy(
                                selection = TextRange(localText.text.length)
                            )
                        },
                    hint = stringResource(Res.string.search_hint),
                    onClear = {
                        focusRequester.requestFocus()
                        onValueChange(localText.copy(""))
                    }
                )
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
