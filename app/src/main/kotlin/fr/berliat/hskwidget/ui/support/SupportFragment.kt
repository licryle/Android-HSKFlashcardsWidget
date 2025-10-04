package fr.berliat.hskwidget.ui.support

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment

import com.google.android.play.core.review.ReviewManager
import com.google.android.play.core.review.ReviewManagerFactory
import fr.berliat.hskwidget.Utils

import fr.berliat.hskwidget.data.store.SupportDevStore
import fr.berliat.hskwidget.ui.screens.support.SupportScreen
import fr.berliat.hskwidget.ui.screens.support.SupportViewModel

class SupportFragment : Fragment() {
    private lateinit var viewModel : SupportViewModel
    private lateinit var reviewManager : ReviewManager

    override fun onResume() {
        super.onResume()

        Utils.logAnalyticsScreenView("SupportDev")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        reviewManager = ReviewManagerFactory.create(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel = SupportViewModel(
            supportDevStore = SupportDevStore.getInstance(requireContext()),
            activityProvider = { this.requireActivity() },
            reviewManager = reviewManager
        )

        return ComposeView(requireContext()).apply {
            setContent {
                SupportScreen(viewModel = viewModel)
            }
        }
    }
}
