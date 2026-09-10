package fr.berliat.hskwidget.ui.screens.config

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

import fr.berliat.hskwidget.core.HSKAppServices
import fr.berliat.hskwidget.ui.components.AppDivider
import fr.berliat.hskwidget.ui.screens.config.ankiSync.AnkiSyncView
import fr.berliat.hskwidget.ui.screens.config.backupCloud.BackupCloudView
import fr.berliat.hskwidget.ui.screens.config.backupDisk.BackupDiskView

@Composable
fun ConfigScreen(
    modifier: Modifier = Modifier,
    viewModel: ConfigViewModel = remember { ConfigViewModel(
        appConfig = HSKAppServices.appPreferences,
        ankiDelegate = HSKAppServices.ankiDelegate,
        gDriveBackup = HSKAppServices.gDriveBackup
    ) }
) {
    val scrollState = rememberScrollState()
    var refreshKey by remember { mutableStateOf(0) }

    val languages = mapOf<String?, String>(
        null to "System",
        "en" to "English",
        //"fr" to "Français",
        "zh-Hans" to "简体中文"
    )

    Column(
        modifier = modifier
            .padding(16.dp)
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        key(refreshKey) {
            LanguageSelectionView(
                languages = languages,
                onLanguageChange = { _ -> refreshKey++ }
            )

            AppDivider()

            BackupDiskView(viewModel = viewModel.backupDiskViewModel)

            AppDivider()

            BackupCloudView(viewModel = viewModel.backupCloudViewModel)

            if (viewModel.ankiSyncViewModel.isAvailableOnThisPlatform) {
                AppDivider()

                AnkiSyncView(viewModel = viewModel.ankiSyncViewModel)
            }
        }
    }
}
