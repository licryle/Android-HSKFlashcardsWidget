package fr.berliat.hskwidget.ui.OCR

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast

import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController

import fr.berliat.hskwidget.data.model.AnnotatedChineseWord
import fr.berliat.hskwidget.domain.Utils

import fr.berliat.hsktextviews.HSKTextSegmenterListener
import fr.berliat.hskwidget.MainActivity
import fr.berliat.hskwidget.core.HSKAppServices
import fr.berliat.hskwidget.ui.dictionary.DictionarySearchFragmentDirections
import fr.berliat.hskwidget.ui.screens.OCR.DisplayOCRScreen
import fr.berliat.hskwidget.ui.screens.OCR.DisplayOCRViewModel

class DisplayOCRFragment : Fragment(), HSKTextSegmenterListener {
    private val viewModel = DisplayOCRViewModel(
        appPreferences = HSKAppServices.appPreferences,
        annotatedChineseWordDAO = HSKAppServices.database.annotatedChineseWordDAO(),
        chineseWordFrequencyDAO = HSKAppServices.database.chineseWordFrequencyDAO()
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val mainApp = requireActivity() as MainActivity

        if (requireActivity().javaClass.simpleName == "MainActivity") {
            mainApp.setOCRReminderVisible()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel.selectedWord.value?.let {
            viewModel.fetchWordForDisplay(it.simplified)
        }

        return ComposeView(requireContext()).apply {
            setContent {
                DisplayOCRScreen(
                    ankiCaller = HSKAppServices.ankiDelegator,
                    viewModel = viewModel,
                    onFavoriteClick = { word -> onFavoriteClick(word) },
                    onClickOCRAdd = {
                        val action =
                            DisplayOCRFragmentDirections.appendOCR(viewModel.text.value)
                        findNavController().navigate(action)
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()

        setupSegmenter()

        Utils.logAnalyticsScreenView("DisplayOCR")
    }

    private fun setupSegmenter() {
        if (!HSKAppServices.HSKSegmenter.isReady()) {
            Log.d(TAG, "Segmenter not ready, setting up listener")
            HSKAppServices.HSKSegmenter.listener = this
        } else {
            Log.d(TAG, "Segmenter ready: let's process arguments")
            processFromArguments()
        }
    }

    private fun processFromArguments() {
        Log.d(TAG, "processFromArguments")

        if (arguments == null) return

        val imageUri = requireArguments().getString("imageUri") ?: ""
        val text = requireArguments().getString("preText") ?: ""
        if (imageUri != "") {
            Log.d(TAG, "processFromArguments: image was provided")

            viewModel.setText(text)

            if (text == "")
                viewModel.resetText()

            viewModel.recognizeText(imageUri)
            requireArguments().putString("imageUri", "") // Consume condition
        } else if (text != "") {
            Log.d(TAG, "processFromArguments: text was provided")

            viewModel.setText(text)
        } else if (viewModel.text.value.isEmpty()) { // text is empty
            Toast.makeText(requireContext(), "Oops - nothing to display", Toast.LENGTH_LONG).show()
        }
    }

    fun onFavoriteClick(word: AnnotatedChineseWord) {
        val action = DictionarySearchFragmentDirections.annotateWord(word.simplified, false)

        findNavController().navigate(action)
    }

    override fun onIsSegmenterReady() {
        Log.d(TAG, "onIsSegmenterReady")
        processFromArguments()
    }

    companion object {
        private const val TAG = "DisplayOCRFragment"

    }
}