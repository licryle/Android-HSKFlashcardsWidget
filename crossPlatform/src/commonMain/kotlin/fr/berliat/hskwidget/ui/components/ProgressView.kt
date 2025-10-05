package fr.berliat.hskwidget.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fr.berliat.hskwidget.Res
import fr.berliat.hskwidget.cancel

import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun ProgressView(
    title: StringResource,
    message: String,
    progress: Float?,
    onClickCancel: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    PrettyCard {
        Column(modifier = modifier.padding(8.dp)) {
            Row(modifier) {
                Text(
                    stringResource(title),
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier) {
                Text(
                    message,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            progress?.let {
                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier) {
                    SmartLinearProgressIndicator(
                        progress = progress,
                        modifier = modifier
                    )
                }
            }

            onClickCancel?.let {
                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Button(
                        onClick = onClickCancel,
                        modifier = modifier
                    ) {
                        Text(stringResource(Res.string.cancel))
                    }
                }
            }
        }
    }
}