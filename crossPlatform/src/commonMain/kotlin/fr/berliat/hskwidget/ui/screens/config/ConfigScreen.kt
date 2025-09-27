package fr.berliat.hskwidget.ui.screens.config

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fr.berliat.hskwidget.ui.screens.config.ankiSync.AnkiSyncView
import fr.berliat.hskwidget.ui.screens.config.backupCloud.BackupCloudView
import fr.berliat.hskwidget.ui.screens.config.backupDisk.BackupDiskView

@Composable
fun ConfigScreen(viewModel: ConfigViewModel) {
    Column(modifier = Modifier.padding(16.dp).fillMaxSize()) {
        BackupDiskView(viewModel.backupDiskViewModel)

        Spacer(modifier = Modifier.height(16.dp))

        BackupCloudView(viewModel.backupCloudViewModel)

        Spacer(modifier = Modifier.height(16.dp))

        AnkiSyncView(viewModel.ankiSyncViewModel)
    }
}