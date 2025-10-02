package fr.berliat.hskwidget.ui.screens.config.backupDisk

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp

import fr.berliat.hskwidget.ui.components.IconButton

import hskflashcardswidget.crossplatform.generated.resources.Res
import hskflashcardswidget.crossplatform.generated.resources.config_backup_activate
import hskflashcardswidget.crossplatform.generated.resources.config_backup_directory_choose
import hskflashcardswidget.crossplatform.generated.resources.config_backup_frequency
import hskflashcardswidget.crossplatform.generated.resources.config_backup_frequency_options
import hskflashcardswidget.crossplatform.generated.resources.config_backup_location
import hskflashcardswidget.crossplatform.generated.resources.config_backup_max_locally
import hskflashcardswidget.crossplatform.generated.resources.config_backup_max_number
import hskflashcardswidget.crossplatform.generated.resources.config_backup_title
import hskflashcardswidget.crossplatform.generated.resources.config_restore_file
import hskflashcardswidget.crossplatform.generated.resources.config_restore_file_choose
import hskflashcardswidget.crossplatform.generated.resources.database_upload_24px
import hskflashcardswidget.crossplatform.generated.resources.file_save_24px
import hskflashcardswidget.crossplatform.generated.resources.folder_open_24px
import hskflashcardswidget.hsktextviews.generated.resources.arrow_dropdown_noborder
import hskflashcardswidget.hsktextviews.generated.resources.pinyinselector_dropdown_icon

import io.github.vinceglb.filekit.name
import org.jetbrains.compose.resources.painterResource

import org.jetbrains.compose.resources.stringArrayResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource

@Composable
fun BackupDiskView(
    viewModel: BackupDiskViewModel,
    modifier: Modifier = Modifier,
    ) {
    val backupDiskActive by viewModel.backupDiskActive.collectAsState()
    val backupDiskFolder by viewModel.backupDiskFolder.collectAsState()
    val backupDiskMaxFiles by viewModel.backupDiskMaxFiles.collectAsState()

    var expanded by remember { mutableStateOf(false) }

    Column {
        Row {
            Icon(
                painter = painterResource(Res.drawable.database_upload_24px),
                contentDescription = stringResource(Res.string.config_backup_title),
                modifier = modifier
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(stringResource(Res.string.config_backup_title),
                style = MaterialTheme.typography.titleMedium)
        }

        Row(verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween) {
            Text(stringResource(Res.string.config_backup_activate))

            Spacer(modifier = Modifier.weight(1f))

            Switch(checked = backupDiskActive, onCheckedChange = viewModel::toggleBackupDiskActive)
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.SpaceBetween) {
            Text(stringResource(Res.string.config_backup_frequency),
                style = MaterialTheme.typography.bodyMedium)

            Spacer(modifier = Modifier.weight(1f))

            Text(stringResource(Res.string.config_backup_frequency_options),
                style = MaterialTheme.typography.bodyMedium)
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.SpaceBetween) {
            Text(stringResource(Res.string.config_backup_max_number),
                style = MaterialTheme.typography.bodyMedium)

            Spacer(modifier = Modifier.weight(1f))

            Box {
                val maxFilesLabels = stringArrayResource(Res.array.config_backup_max_locally)
                val maxFiles = maxFilesLabels.associateBy { label -> label.substringBefore(" ").toInt() }

                Row(
                    modifier = Modifier.clickable { expanded = true }
                ) {
                    Text(text = maxFiles.getValue(backupDiskMaxFiles), style = TextStyle.Default)

                    Icon(
                        painter = rememberVectorPainter(vectorResource(hskflashcardswidget.hsktextviews.generated.resources.Res.drawable.arrow_dropdown_noborder)),
                        contentDescription = stringResource(hskflashcardswidget.hsktextviews.generated.resources.Res.string.pinyinselector_dropdown_icon),
                        modifier = Modifier.size(10.dp)
                    )
                }

                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    maxFiles.map {
                        DropdownMenuItem(
                            text = { Text(it.value, style = TextStyle.Default) },
                            onClick = {
                                viewModel.setBackupDiskMaxFiles(it.key)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween) {
            Text(stringResource(Res.string.config_backup_location),
                style = MaterialTheme.typography.bodyMedium)

            Spacer(modifier = Modifier.weight(1f))

            val dirText = backupDiskFolder?.name ?: stringResource(Res.string.config_backup_directory_choose)
            IconButton(
                onClick = viewModel::selectBackupFolder,
                text = dirText,
                drawable = Res.drawable.folder_open_24px
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween) {
            Text(stringResource(Res.string.config_restore_file),
                style = MaterialTheme.typography.bodyMedium)

            Spacer(modifier = Modifier.weight(1f))

            IconButton(
                onClick = viewModel::selectRestoreFile,
                text = stringResource(Res.string.config_restore_file_choose),
                drawable = Res.drawable.file_save_24px
            )
        }
    }
}