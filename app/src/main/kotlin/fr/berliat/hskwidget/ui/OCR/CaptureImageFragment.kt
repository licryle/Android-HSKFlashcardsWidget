package fr.berliat.hskwidget.ui.OCR

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import fr.berliat.hskwidget.Utils

import fr.berliat.hskwidget.ui.screens.OCR.CaptureImageScreen
import fr.berliat.hskwidget.ui.screens.OCR.CaptureImageViewModel

class CaptureImageFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                CaptureImageScreen(
                    viewModel = CaptureImageViewModel { it -> redirectOnImageReady(it) }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Utils.logAnalyticsScreenView("CaptureImage")
    }

    fun redirectOnImageReady(filePath: String) {
        /*val action = CaptureImageFragmentDirections.displayOCR(
            filePath,
            arguments?.getString("preText") ?: "")
        findNavController().navigate(action)*/
    }
}