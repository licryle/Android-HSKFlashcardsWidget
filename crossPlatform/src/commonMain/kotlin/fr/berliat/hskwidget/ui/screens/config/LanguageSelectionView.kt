package fr.berliat.hskwidget.ui.screens.config

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fr.berliat.hskwidget.Res
import fr.berliat.hskwidget.config_language
import fr.berliat.hskwidget.core.LocaleManager
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageSelectionView(
    modifier: Modifier = Modifier
) {
    var currentLanguage by remember { mutableStateOf(LocaleManager.getCurrentLanguage()) }
    var expanded by remember { mutableStateOf(false) }
    
    val languages = mapOf<String?, String>(
        null to "System",
        "en" to "English",
        "fr" to "Français",
        "zh-Hans" to "简体中文"
    )

    Row(verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween) {
        Text(stringResource(Res.string.config_language),
            style = MaterialTheme.typography.bodyMedium)

        Spacer(modifier = Modifier.weight(1f))

        Box {
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it },
                modifier = Modifier.width(160.dp)
            ) {
                OutlinedTextField(
                    readOnly = true,
                    value = languages.getValue(LocaleManager.getCurrentLanguage()),
                    textStyle = MaterialTheme.typography.bodyMedium,
                    onValueChange = {},
                    modifier = Modifier
                        .menuAnchor(MenuAnchorType.PrimaryEditable, true),
                    label = {
                        Text(
                            stringResource(Res.string.config_language),
                            style = MaterialTheme.typography.bodySmall
                        )
                    },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }) {
                    languages.forEach { (code, name) ->
                        DropdownMenuItem(
                            text = { Text(name, style = MaterialTheme.typography.bodyMedium) },
                            onClick = {
                                LocaleManager.setLanguage(code)
                                currentLanguage = code
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}