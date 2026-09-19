package fr.berliat.hskwidget.ui.screens.config

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

import fr.berliat.hskwidget.Res
import fr.berliat.hskwidget.config_repair_db_action
import fr.berliat.hskwidget.config_repair_db_description
import fr.berliat.hskwidget.config_repair_db_title
import fr.berliat.hskwidget.core.HSKAppServices
import fr.berliat.hskwidget.core.LocaleManager
import fr.berliat.hskwidget.database_updating_progress
import fr.berliat.hskwidget.domain.DatabaseHelper
import fr.berliat.hskwidget.refresh_24px
import fr.berliat.hskwidget.ui.components.AppDivider
import fr.berliat.hskwidget.ui.components.IconButton
import fr.berliat.hskwidget.ui.screens.config.ankiSync.AnkiSyncView
import fr.berliat.hskwidget.ui.screens.config.backupCloud.BackupCloudView
import fr.berliat.hskwidget.ui.screens.config.backupDisk.BackupDiskView
import fr.berliat.hskwidget.ui.widget.FlashcardWidgetProvider
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun ConfigScreen(
    modifier: Modifier = Modifier,
    viewModel: ConfigViewModel = remember { ConfigViewModel(
        appConfig = HSKAppServices.appPreferences,
        widgetProvider = FlashcardWidgetProvider(),
        ankiDelegate = HSKAppServices.ankiDelegate,
        gDriveBackup = HSKAppServices.gDriveBackup
    ) }
) {
    val scrollState = rememberScrollState()
    var refreshKey by remember { mutableStateOf(0) }
    val updateProgress by DatabaseHelper.updateProgress.collectAsState()

    Column(
        modifier = modifier
            .padding(16.dp)
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        key(refreshKey) {
            LocaleSelectionView(
                localeManager = LocaleManager,
                onLocaleChange = { _ -> run {
                        refreshKey++
                        viewModel.onLanguageChange()
                    }
                }
            )

            AppDivider()

            BackupDiskView(viewModel = viewModel.backupDiskViewModel)

            AppDivider()

            BackupCloudView(viewModel = viewModel.backupCloudViewModel)

            if (viewModel.ankiSyncViewModel.isAvailableOnThisPlatform) {
                AppDivider()

                AnkiSyncView(viewModel = viewModel.ankiSyncViewModel)
            }

            AppDivider()

            RepairDatabaseView(
                onRepair = viewModel::repairDatabase,
                updateProgress = updateProgress
            )
        }
    }
}

@Composable
private fun RepairDatabaseView(
    onRepair: () -> Unit,
    updateProgress: Float?,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Icon(
                painter = painterResource(Res.drawable.refresh_24px),
                contentDescription = stringResource(Res.string.config_repair_db_title)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = stringResource(Res.string.config_repair_db_title),
                style = MaterialTheme.typography.titleMedium
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(Res.string.config_repair_db_description),
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            val btnText = if (updateProgress != null) {
                stringResource(Res.string.database_updating_progress, updateProgress.toInt())
            } else {
                stringResource(Res.string.config_repair_db_action)
            }

            IconButton(
                onClick = onRepair,
                text = btnText,
                drawable = Res.drawable.refresh_24px,
                enabled = updateProgress == null
            )
        }
    }
}
