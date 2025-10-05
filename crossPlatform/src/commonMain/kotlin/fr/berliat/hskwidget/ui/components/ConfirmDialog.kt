package fr.berliat.hskwidget.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

import fr.berliat.hskwidget.Res
import fr.berliat.hskwidget.cancel
import fr.berliat.hskwidget.proceed

import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun ConfirmDialog(
    title: StringResource,
    message: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    confirmButtonLabel: StringResource = Res.string.proceed,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        modifier = modifier,
        onDismissRequest = { onDismiss() },
        title = {
            Text(text = stringResource(title))
        },
        text = {
            Text(text = message)
        },
        confirmButton = {
            TextButton(onClick = { onConfirm() }) {
                Text(stringResource(confirmButtonLabel))
            }
        },
        dismissButton = {
            TextButton(onClick = { onDismiss() }) {
                Text(stringResource(Res.string.cancel))
            }
        }
    )
}