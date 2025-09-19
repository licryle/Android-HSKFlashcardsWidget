package fr.berliat.hskwidget.ui.about

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import fr.berliat.hskwidget.ui.screens.about.AboutScreen
import fr.berliat.hskwidget.ui.screens.about.AboutViewModel

class AboutFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                AboutScreen(AboutViewModel()) // Works now
            }
        }
    }
}