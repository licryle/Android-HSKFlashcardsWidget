package fr.berliat.hskwidget

import androidx.compose.ui.window.ComposeUIViewController
import fr.berliat.hskwidget.ui.application.AppView
import fr.berliat.hskwidget.ui.application.AppViewModel
import fr.berliat.hskwidget.ui.navigation.NavigationManager

fun AppViewController() = ComposeUIViewController {
    val viewModel = AppViewModel { /* platform context if needed */ null }
    AppView(
        navigationManager = NavigationManager,
        viewModel = viewModel
    )
}