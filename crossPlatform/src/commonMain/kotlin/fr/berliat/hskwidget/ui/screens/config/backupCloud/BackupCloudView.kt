package fr.berliat.hskwidget.ui.screens.config.backupCloud

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

import fr.berliat.hskwidget.YYMMDDHHMMSS
import fr.berliat.hskwidget.core.HSKAppServices
import fr.berliat.hskwidget.fromKBToMB
import fr.berliat.hskwidget.ui.components.ConfirmDialog
import fr.berliat.hskwidget.ui.components.IconButton
import fr.berliat.hskwidget.ui.components.ProgressView

import hskflashcardswidget.crossplatform.generated.resources.Res
import hskflashcardswidget.crossplatform.generated.resources.cloud_download_24px
import hskflashcardswidget.crossplatform.generated.resources.cloud_upload_24px
import hskflashcardswidget.crossplatform.generated.resources.config_backup_cloud_backupnow
import hskflashcardswidget.crossplatform.generated.resources.config_backup_cloud_lastone
import hskflashcardswidget.crossplatform.generated.resources.config_backup_cloud_lastone_never
import hskflashcardswidget.crossplatform.generated.resources.config_backup_cloud_restorenow
import hskflashcardswidget.crossplatform.generated.resources.config_backup_cloud_title
import hskflashcardswidget.crossplatform.generated.resources.googledrive_backup_cancel_message
import hskflashcardswidget.crossplatform.generated.resources.googledrive_backup_cancel_title
import hskflashcardswidget.crossplatform.generated.resources.googledrive_backup_failed_message
import hskflashcardswidget.crossplatform.generated.resources.googledrive_backup_failed_title
import hskflashcardswidget.crossplatform.generated.resources.googledrive_backup_progress_message
import hskflashcardswidget.crossplatform.generated.resources.googledrive_backup_start_message
import hskflashcardswidget.crossplatform.generated.resources.googledrive_backup_start_title
import hskflashcardswidget.crossplatform.generated.resources.googledrive_backup_success_message
import hskflashcardswidget.crossplatform.generated.resources.googledrive_backup_success_title
import hskflashcardswidget.crossplatform.generated.resources.googledrive_restore_cancel_message
import hskflashcardswidget.crossplatform.generated.resources.googledrive_restore_cancel_title
import hskflashcardswidget.crossplatform.generated.resources.googledrive_restore_confirm_overwrite
import hskflashcardswidget.crossplatform.generated.resources.googledrive_restore_failed_message
import hskflashcardswidget.crossplatform.generated.resources.googledrive_restore_failed_title
import hskflashcardswidget.crossplatform.generated.resources.googledrive_restore_progress_message
import hskflashcardswidget.crossplatform.generated.resources.googledrive_restore_start_message
import hskflashcardswidget.crossplatform.generated.resources.googledrive_restore_start_title
import hskflashcardswidget.crossplatform.generated.resources.googledrive_restore_success_message
import hskflashcardswidget.crossplatform.generated.resources.googledrive_restore_success_title

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun BackupCloudView(
    modifier: Modifier = Modifier,
    viewModel: BackupCloudViewModel = remember { BackupCloudViewModel(
        HSKAppServices.appPreferences,
        HSKAppServices.gDriveBackup
    ) }
) {
    val lastCloudUpdate = viewModel.cloudLastBackup.collectAsState()
    val busy = viewModel.isBusy.collectAsState()
    val transferState = viewModel.transferState.collectAsState()
    val restoreFileFrom = viewModel.restoreFileFrom.collectAsState()

    val restoreFileTime = restoreFileFrom.value
    restoreFileTime?.let {
        ConfirmDialog(
            title = Res.string.config_backup_cloud_restorenow,
            message = stringResource(Res.string.googledrive_restore_confirm_overwrite, restoreFileTime.YYMMDDHHMMSS()),
            onConfirm = {
                viewModel.restoreFileFrom.value = null
                viewModel.confirmRestoration()
            },
            onDismiss = { viewModel.restoreFileFrom.value = null }
        )
    }

    Column {
        Row {
            Icon(
                painter = painterResource(Res.drawable.cloud_upload_24px),
                contentDescription = stringResource(Res.string.config_backup_cloud_title),
                modifier = modifier
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                stringResource(Res.string.config_backup_cloud_title),
                style = MaterialTheme.typography.titleMedium
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(stringResource(Res.string.config_backup_cloud_lastone))

            Spacer(modifier = Modifier.weight(1f))

            val lastUpdate = if (lastCloudUpdate.value == Instant.fromEpochMilliseconds(0)) {
                stringResource(Res.string.config_backup_cloud_lastone_never)
            } else {
                lastCloudUpdate.value.YYMMDDHHMMSS()
            }
            Text(lastUpdate)
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        )  {
            IconButton(
                text = stringResource(Res.string.config_backup_cloud_backupnow),
                onClick = viewModel::backup,
                drawable = Res.drawable.cloud_upload_24px,
                enabled = ! busy.value
            )

            Spacer(modifier = Modifier.weight(1f))

            IconButton(
                text = stringResource(Res.string.config_backup_cloud_restorenow),
                onClick = viewModel::restore,
                drawable = Res.drawable.cloud_download_24px,
                enabled = ! busy.value
            )
        }


        val tState = transferState.value
        tState?.let {
            var title: StringResource
            var message = ""
            var click : (() -> Unit)? = { viewModel.cancel() }
            var progress: Float? = -1f

            when (tState) {
                is BackupCloudTransferEvent.BackupStarted -> {
                    title = Res.string.googledrive_backup_start_title
                    message = stringResource(Res.string.googledrive_backup_start_message)
                }
                is BackupCloudTransferEvent.BackupProgress -> {
                    title = Res.string.googledrive_backup_start_title
                    message = stringResource(Res.string.googledrive_backup_progress_message,
                        tState.bytesReceived.fromKBToMB(), tState.bytesTotal.fromKBToMB())
                    progress = tState.bytesReceived / tState.bytesTotal.toFloat()
                }
                is BackupCloudTransferEvent.BackupCancelled -> {
                    title = Res.string.googledrive_backup_cancel_title
                    message = stringResource(Res.string.googledrive_backup_cancel_message)
                    click = null
                    progress = null
                }
                is BackupCloudTransferEvent.BackupFailed -> {
                    title = Res.string.googledrive_backup_failed_title
                    message = stringResource(Res.string.googledrive_backup_failed_message,
                        tState.exception.message ?: "")
                    click = null
                    progress = null
                }
                is BackupCloudTransferEvent.BackupSuccess -> {
                    title = Res.string.googledrive_backup_success_title
                    message = stringResource(Res.string.googledrive_backup_success_message,
                        Clock.System.now().YYMMDDHHMMSS())
                    click = null
                    progress = null
                }

                is BackupCloudTransferEvent.RestorationStarted -> {
                    title = Res.string.googledrive_restore_start_title
                    message = stringResource(Res.string.googledrive_restore_start_message)
                }
                is BackupCloudTransferEvent.RestorationProgress -> {
                    title = Res.string.googledrive_restore_start_title
                    message = stringResource(Res.string.googledrive_restore_progress_message,
                        tState.bytesReceived.fromKBToMB(), tState.bytesTotal.fromKBToMB())
                    progress = tState.bytesReceived / tState.bytesTotal.toFloat()
                }
                is BackupCloudTransferEvent.RestorationCancelled -> {
                    title = Res.string.googledrive_restore_cancel_title
                    message = stringResource(Res.string.googledrive_restore_cancel_message)
                    click = null
                    progress = null
                }
                is BackupCloudTransferEvent.RestorationFailed -> {
                    title = Res.string.googledrive_restore_failed_title
                    message = stringResource(Res.string.googledrive_restore_failed_message,
                        tState.exception.message ?: "")
                    click = null
                    progress = null
                }
                is BackupCloudTransferEvent.RestorationSuccess -> {
                    title = Res.string.googledrive_restore_success_title
                    message = stringResource(Res.string.googledrive_restore_success_message,
                        Clock.System.now().YYMMDDHHMMSS())
                    click = null
                    progress = null
                }
            }

            ProgressView(
                title = title,
                message = message,
                progress = progress,
                onClickCancel = click
            )
        }
    }
}