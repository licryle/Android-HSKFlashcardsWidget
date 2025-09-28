package fr.berliat.hskwidget.ui.config

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment

import fr.berliat.hskwidget.domain.Utils
import fr.berliat.hskwidget.ui.screens.config.ConfigScreen
import fr.berliat.hskwidget.ui.screens.config.ConfigViewModel
import fr.berliat.hskwidget.ui.utils.HSKAnkiDelegate

class ConfigFragment : Fragment() {
    private lateinit var ankiDelegate: HSKAnkiDelegate

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ankiDelegate = HSKAnkiDelegate(this)
    }

    override fun onResume() {
        super.onResume()

        Utils.logAnalyticsScreenView("Config")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val viewModel = ConfigViewModel(
            ankiDelegate = ankiDelegate::delegateToAnki
        )

        return ComposeView(requireContext()).apply {
            setContent {
                ConfigScreen(viewModel = viewModel)
            }
        }
    }
}