package fr.berliat.hskwidget

import android.content.Intent
import android.os.Bundle

import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity

import fr.berliat.hskwidget.ui.application.AppView
import fr.berliat.hskwidget.ui.navigation.NavigationManager
import fr.berliat.hskwidget.ui.application.AppViewModel

class MainActivity : AppCompatActivity() {
    private lateinit var viewModel: AppViewModel

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        viewModel.handleIntent(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = AppViewModel { this }

        viewModel.handleIntent(intent)

        setContent {
            AppView(
                navigationManager = NavigationManager,
                viewModel = viewModel
            )
        }
    }
}