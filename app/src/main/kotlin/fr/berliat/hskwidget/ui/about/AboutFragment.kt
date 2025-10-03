package fr.berliat.hskwidget.ui.about

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import fr.berliat.hskwidget.domain.Utils
import fr.berliat.hskwidget.ui.screens.about.AboutScreen

class AboutFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        return ComposeView(requireContext()).apply {
            setContent {
                AboutScreen()
            }
        }
    }

    override fun onResume() {
        super.onResume()

        Utils.logAnalyticsScreenView("About")
    }
}