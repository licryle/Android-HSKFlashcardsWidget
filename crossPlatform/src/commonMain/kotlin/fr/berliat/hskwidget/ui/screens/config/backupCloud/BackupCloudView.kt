package fr.berliat.hskwidget.ui.screens.config.backupCloud

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

import fr.berliat.hskwidget.YYMMDDHHMMSS

@Composable
fun BackupCloudView(
    viewModel: BackupCloudViewModel,
    modifier: Modifier = Modifier,
) {
    Column {
       /* Text("Cloud Backup")
        Spacer(modifier = Modifier.height(8.dp))
        Row {
            Button(onClick = onBackupNow, enabled = !backupInProgress) { Text("Backup Now") }
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = onRestoreNow, enabled = !backupInProgress) { Text("Restore Now") }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text("Last Backup: ${lastBackup.YYMMDDHHMMSS()}")

        if (backupInProgress) {
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }*/
    }
}