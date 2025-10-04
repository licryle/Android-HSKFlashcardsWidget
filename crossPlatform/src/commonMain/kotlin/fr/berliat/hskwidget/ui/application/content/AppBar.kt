package fr.berliat.hskwidget.ui.application.content

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier

import hskflashcardswidget.crossplatform.generated.resources.Res
import hskflashcardswidget.crossplatform.generated.resources.menu_ocr
import hskflashcardswidget.crossplatform.generated.resources.photo_camera_24px
import hskflashcardswidget.crossplatform.generated.resources.search_hint

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
    val searchQuery by viewModel.searchQuery.collectAsState()

    fun onValueChange(s: String) {
        viewModel.updateSearchQuery(s)
        onSearch(s)
    }

    TopAppBar(
        title = {
            Text(title)

            TextField(
                value = searchQuery,
                onValueChange = { onValueChange(it) },
                placeholder = { Text(stringResource(Res.string.search_hint)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onValueChange("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear text")
                        }
                    }
                }
            )
        },
        navigationIcon = {
            IconButton(onClick = onMenuClick) {
                Icon(Icons.Default.Menu, contentDescription = "Menu")
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
