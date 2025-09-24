package fr.berliat.hskwidget.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun LoadingProgressView(
    modifier: Modifier = Modifier
) {
    CircularProgressIndicator(
        modifier = modifier.size(50.dp),
        strokeWidth = 4.dp
    )
}