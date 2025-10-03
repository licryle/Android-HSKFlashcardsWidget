package fr.berliat.hskwidget.ui.config

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import fr.berliat.hskwidget.core.HSKAppServices

import fr.berliat.hskwidget.domain.Utils
import fr.berliat.hskwidget.ui.screens.config.ConfigScreen
import fr.berliat.hskwidget.ui.screens.config.ConfigViewModel

class ConfigFragment : Fragment() {
    override fun onResume() {
        super.onResume()

        Utils.logAnalyticsScreenView("Config")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val ankiDelegate = HSKAppServices.ankiDelegate
        val viewModel = ConfigViewModel(
            ankiDelegate = ankiDelegate::delegateToAnkiService,
            gDriveBackup = HSKAppServices.gDriveBackup
        )
        ankiDelegate.replaceListener(viewModel.ankiSyncViewModel)

        return ComposeView(requireContext()).apply {
            setContent {
                ConfigScreen(viewModel = viewModel)
            }
        }
    }
}