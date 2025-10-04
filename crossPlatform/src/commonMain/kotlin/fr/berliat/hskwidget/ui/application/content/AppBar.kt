package fr.berliat.hskwidget.ui.application.content

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    onMenuClick: () -> Unit
) {
    TopAppBar(
        title = {
            // Replace SearchView with a Compose TextField
            var query by remember { mutableStateOf("") }

            Text(title)

            TextField(
                value = query,
                onValueChange = {
                    query = it
                    onSearch(query)
                },
                placeholder = { Text(stringResource(Res.string.search_hint)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.onSurface),
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
