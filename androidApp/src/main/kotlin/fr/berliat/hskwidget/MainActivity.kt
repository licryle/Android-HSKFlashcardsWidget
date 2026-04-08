package fr.berliat.hskwidget

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.core.view.WindowInsetsControllerCompat

import fr.berliat.hskwidget.ui.application.AppIntentBus
import fr.berliat.hskwidget.ui.application.AppView
import fr.berliat.hskwidget.ui.application.AppViewModel
import fr.berliat.hskwidget.ui.navigation.NavigationManager

class MainActivity : AppCompatActivity() {
    private lateinit var viewModel: AppViewModel

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        AppViewModel.convertToAppIntent(intent)?.let {
            AppIntentBus.emit(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = AppViewModel(NavigationManager) { this }
        viewModel.init()

        AppViewModel.convertToAppIntent(intent)?.let {
            AppIntentBus.emit(it)
        }

        setContent {
            configureSystemBars(isSystemInDarkTheme())

            AppView(viewModel = viewModel)
        }
    }

    private fun configureSystemBars(isDarkTheme: Boolean) {
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.isAppearanceLightStatusBars = !isDarkTheme
        controller.isAppearanceLightNavigationBars = !isDarkTheme
    }
}