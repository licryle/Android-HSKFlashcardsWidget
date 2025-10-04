package fr.berliat.hskwidget.ui.screens.config

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fr.berliat.hskwidget.core.HSKAppServices
import fr.berliat.hskwidget.ui.screens.config.ankiSync.AnkiSyncView
import fr.berliat.hskwidget.ui.screens.config.backupCloud.BackupCloudView
import fr.berliat.hskwidget.ui.screens.config.backupDisk.BackupDiskView

@Composable
fun ConfigScreen(
    viewModel: ConfigViewModel = remember { ConfigViewModel(
        appConfig = HSKAppServices.appPreferences,
        ankiDelegate = HSKAppServices.ankiDelegate,
        gDriveBackup = HSKAppServices.gDriveBackup
    ) }
) {
    Column(modifier = Modifier.padding(16.dp).fillMaxSize()) {
        BackupDiskView(viewModel = viewModel.backupDiskViewModel)

        Spacer(modifier = Modifier.height(16.dp))

        BackupCloudView(viewModel = viewModel.backupCloudViewModel)

        Spacer(modifier = Modifier.height(16.dp))

        AnkiSyncView(viewModel = viewModel.ankiSyncViewModel)
    }
}