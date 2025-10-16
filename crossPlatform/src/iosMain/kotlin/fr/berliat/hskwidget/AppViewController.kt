package fr.berliat.hskwidget

import androidx.compose.ui.window.ComposeUIViewController

import fr.berliat.hskwidget.ui.application.AppView
import fr.berliat.hskwidget.ui.application.AppViewModel
import fr.berliat.hskwidget.ui.navigation.NavigationManager
import platform.UIKit.UIViewController

private val viewModel = AppViewModel(NavigationManager)

fun AppViewController(): UIViewController = ComposeUIViewController(
		configure = {
			// This will prevent the crash by disabling the strict plist sanity check
			enforceStrictPlistSanityCheck = false
		}
	) {
    // This is a composable context, so we can call our composable functions
    AppView(
        viewModel = viewModel
    )
}
