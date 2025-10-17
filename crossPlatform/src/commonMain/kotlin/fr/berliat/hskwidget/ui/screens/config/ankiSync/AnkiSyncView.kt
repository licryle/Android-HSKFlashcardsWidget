package fr.berliat.hskwidget.ui.screens.config.ankiSync

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import fr.berliat.hskwidget.core.HSKAppServices

@Composable
expect fun AnkiSyncView(
    modifier: Modifier = Modifier,
    viewModel: AnkiSyncViewModel = remember {
        AnkiSyncViewModel(HSKAppServices.ankiDelegate,
            HSKAppServices.appPreferences) }
    )