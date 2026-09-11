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
fun LocaleSelectionView(
    localeManager: LocaleManager,
    modifier: Modifier = Modifier,
    onLanguageChange: (String?) -> Unit = {}
) {
    var currentLanguage by remember { mutableStateOf(localeManager.getCurrentLocale()) }
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
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
                    value = localeManager.supportedLocales[localeManager.getCurrentLocale()] ?: "Error",
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
                    localeManager.supportedLocales.forEach { (code, name) ->
                        DropdownMenuItem(
                            text = { Text(name, style = MaterialTheme.typography.bodyMedium) },
                            onClick = {
                                LocaleManager.setLocale(code)
                                currentLanguage = code
                                expanded = false
                                onLanguageChange(code)
                            }
                        )
                    }
                }
            }
        }
    }
}
