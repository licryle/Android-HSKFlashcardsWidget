package fr.berliat.hskwidget.ui.screens.config.ankiSync

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import fr.berliat.hskwidget.core.HSKAppServices

import fr.berliat.hskwidget.ui.components.ProgressView

import fr.berliat.hskwidget.Res
import fr.berliat.hskwidget.anki_icon
import fr.berliat.hskwidget.anki_sync_cancelled_message
import fr.berliat.hskwidget.anki_sync_cancelled_title
import fr.berliat.hskwidget.anki_sync_failure_message
import fr.berliat.hskwidget.anki_sync_failure_title
import fr.berliat.hskwidget.anki_sync_start_message
import fr.berliat.hskwidget.anki_sync_start_title
import fr.berliat.hskwidget.anki_sync_success_message
import fr.berliat.hskwidget.anki_sync_success_title
import fr.berliat.hskwidget.config_anki_activate
import fr.berliat.hskwidget.config_anki_integration
import fr.berliat.hskwidget.config_anki_title

import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun AnkiSyncView(
    modifier: Modifier = Modifier,
    viewModel: AnkiSyncViewModel = remember {
        AnkiSyncViewModel(HSKAppServices.ankiDelegate,
            HSKAppServices.appPreferences) }
    ) {
    if (!viewModel.isAvailableOnThisPlatform) return

    val ankiActive by viewModel.ankiActive.collectAsState()
    val syncProgress by viewModel.syncProgress.collectAsState()

    Column {
        Row(modifier = Modifier.padding(bottom = 8.dp)) {
            Icon(
                painter = painterResource(Res.drawable.anki_icon),
                contentDescription = stringResource(Res.string.config_anki_title),
                modifier = modifier,
                tint = Color.Unspecified
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                stringResource(Res.string.config_anki_title),
                style = MaterialTheme.typography.titleLarge
            )
        }

        Row {
            Text(
                stringResource(Res.string.config_anki_integration),
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(stringResource(Res.string.config_anki_activate))

            Spacer(modifier = Modifier.weight(1f))

            Switch(checked = ankiActive, onCheckedChange = viewModel::toggleAnkiActive)
        }

        AnkiSyncProgress(
            syncProgress = syncProgress,
            onClickCancel = viewModel::cancelSync,
            modifier = modifier
        )
    }
}

@Composable
internal fun AnkiSyncProgress(
    syncProgress: SyncProgress,
    onClickCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiStateData =
        when (syncProgress.state) {
            SyncState.SUCCESS -> {
                UIStateData(
                    titleRes = Res.string.anki_sync_success_title,
                    message = stringResource(Res.string.anki_sync_success_message, syncProgress.message),
                    progress = null,
                    cancelClick = null
                )
            }

            SyncState.STARTING -> {
                UIStateData(
                    titleRes = Res.string.anki_sync_start_title,
                    message = stringResource(Res.string.anki_sync_start_message),
                    progress = -1f,
                    cancelClick = onClickCancel
                )
            }

            SyncState.CANCELLED -> {
                UIStateData(
                    titleRes = Res.string.anki_sync_cancelled_title,
                    message = stringResource(Res.string.anki_sync_cancelled_message),
                    progress = null,
                    cancelClick = null
                )
            }

            SyncState.FAILED -> {
                UIStateData(
                    titleRes = Res.string.anki_sync_failure_title,
                    message = stringResource(Res.string.anki_sync_failure_message, syncProgress.message),
                    cancelClick = null,
                    progress = null
                )
            }

            SyncState.IN_PROGRESS -> {
                UIStateData(
                    titleRes = Res.string.anki_sync_start_title,
                    message = syncProgress.message,
                    progress = syncProgress.current / syncProgress.total.toFloat(),
                    cancelClick = onClickCancel
                )
            }

            else -> {
                null
            }
        }

    uiStateData?.let {
        ProgressView(
            title = uiStateData.titleRes,
            message = uiStateData.message,
            progress = uiStateData.progress,
            onClickCancel = uiStateData.cancelClick,
            modifier = modifier
        )
    }
}

internal data class UIStateData(
    val titleRes: StringResource,
    val message: String,
    val progress: Float?,
    val cancelClick: (() -> Unit)?
)