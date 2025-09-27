package fr.berliat.hskwidget.ui.screens.config.ankiSync

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Checkbox
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AnkiSyncView(
    viewModel: AnkiSyncViewModel,
    modifier: Modifier = Modifier
    ) {
    Column {
        /*Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Checkbox(checked = active, onCheckedChange = onToggleActive)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Anki Integration")
        }

        if (progress != null) {
            Spacer(modifier = Modifier.height(8.dp))
            val (current, total) = progress
            LinearProgressIndicator(progress = if (total > 0) current / total.toFloat() else 0f, modifier = Modifier.fillMaxWidth())
            Text("Progress: $current / $total")
        }*/
    }
}