package fr.berliat.hskwidget.ui.OCR

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import fr.berliat.hsktextviews.views.HSKTextView
import fr.berliat.hskwidget.R
import androidx.compose.runtime.livedata.observeAsState

import fr.berliat.hskwidget.data.model.AnnotatedChineseWord
import fr.berliat.hskwidget.data.store.AppPreferencesStore
import fr.berliat.hskwidget.databinding.FragmentOcrDisplayBinding
import fr.berliat.hskwidget.domain.Utils
import androidx.core.net.toUri
import com.google.mlkit.vision.common.InputImage
import fr.berliat.hsktextviews.HSKTextSegmenter
import fr.berliat.hsktextviews.HSKTextSegmenterListener
import fr.berliat.hsktextviews.views.ShowPinyins
import fr.berliat.hskwidget.MainActivity
import fr.berliat.hskwidget.data.store.DatabaseHelper
import fr.berliat.hskwidget.domain.SharedViewModel
import fr.berliat.hskwidget.domain.hanziClickedBackground
import fr.berliat.hskwidget.domain.hanziStyle
import fr.berliat.hskwidget.domain.pinyinStyle
import kotlin.collections.emptyMap

class DisplayOCRFragment : Fragment(), HSKTextSegmenterListener {
    private lateinit var viewBinding: FragmentOcrDisplayBinding
    private lateinit var segmenter: HSKTextSegmenter
    private lateinit var viewModel: DisplayOCRViewModel

    private lateinit var appConfig: AppPreferencesStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        segmenter = SharedViewModel.getInstance(this).segmenter

        val mainApp = requireActivity() as MainActivity
        val factory = DisplayOCRViewModelFactory(
            { DatabaseHelper.getInstance(requireContext()).annotatedChineseWordDAO() },
            { DatabaseHelper.getInstance(requireContext()).chineseWordFrequencyDAO() }
        )
        viewModel = ViewModelProvider(this, factory)[DisplayOCRViewModel::class.java]

        if (requireActivity().javaClass.simpleName == "MainActivity") {
            mainApp.setOCRReminderVisible()
        }
    }

    @Composable
    fun OCRDisplayLoading(
        modifier: Modifier = Modifier,
        backgroundColor: Color = Color(0xFFFFFFFF), // replace with theme color if needed
        loadingText: String = stringResource(id = R.string.ocr_display_loading)
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .fillMaxHeight(0.5f) // adjust if needed
                .background(backgroundColor)
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(50.dp),
                strokeWidth = 4.dp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = loadingText,
                textAlign = TextAlign.Center
            )
        }
    }

    @Composable
    fun OCRDisplayEmpty() {
        Text("No text returned")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewBinding =
            FragmentOcrDisplayBinding.inflate(inflater, container, false) // Inflate here
        appConfig = AppPreferencesStore(requireContext())

        viewBinding.ocrDisplayAdd.setOnClickListener {
            val action =
                DisplayOCRFragmentDirections.appendOCR(viewModel.text.value ?: "")
            findNavController().navigate(action)
        }

        val composeView = viewBinding.ocrDisplayText
        composeView.apply {
            // Make sure composition lifecycle follows the View lifecycle
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
            )

            setContent {
                val text by viewModel.text.observeAsState("")
                val clickedWords by viewModel.clickedWords.observeAsState(emptyMap())

                // Call your composable here
                HSKTextView(
                    text = text,
                    segmenter = segmenter,
                    hanziStyle = hanziStyle.copy(fontSize = appConfig.readerTextSize.sp),
                    pinyinStyle = pinyinStyle,
                    clickedHanziStyle = hanziStyle.copy(fontSize = appConfig.readerTextSize.sp),
                    clickedPinyinStyle = pinyinStyle,
                    clickedBackgroundColor = hanziClickedBackground,
                    loadingComposable = {  OCRDisplayLoading() },
                    emptyComposable = { OCRDisplayEmpty() },
                    onWordClick = { word -> this@DisplayOCRFragment.onWordClick(word) },
                    showPinyins = if (appConfig.readerShowAllPinyins) ShowPinyins.ALL else ShowPinyins.CLICKED,
                    endSeparator = if (appConfig.readerSeparateWords) WORD_SEPARATOR else "",
                    clickedWords = clickedWords,
                    onTextAnalysisFailure = { e -> this@DisplayOCRFragment.onTextAnalysisFailure(e) }
                )
            }
        }

        viewBinding.ocrDisplayConfBigger.setOnClickListener { updateTextSize(2) }
        viewBinding.ocrDisplayConfSmaller.setOnClickListener { updateTextSize(-2) }

        viewBinding.ocrDisplaySeparator.isChecked = appConfig.readerSeparateWords
        viewBinding.ocrDisplaySeparator.setOnClickListener {
            appConfig.readerSeparateWords = viewBinding.ocrDisplaySeparator.isChecked
        }

        viewBinding.ocrDisplayPinyins.isChecked = appConfig.readerShowAllPinyins
        viewBinding.ocrDisplayPinyins.setOnClickListener {
            appConfig.readerShowAllPinyins = viewBinding.ocrDisplayPinyins.isChecked
        }

        viewModel.toastEvent.observe(viewLifecycleOwner) { value ->
            if (value.first.isNotEmpty()) {
                Toast.makeText(requireContext(), value.first, value.second).show()
            }
        }

        setupSegmenter()

        return viewBinding.root // Return the root view of the binding
    }

    override fun onResume() {
        super.onResume()

        if (viewModel.selectedWord != null) {
            viewModel.fetchWordForDisplay(viewModel.selectedWord!!, ::showSelectedWord)
        }

        Utils.logAnalyticsScreenView("DisplayOCR")
    }

    private fun setupSegmenter() {
        if (! segmenter.isReady()) {
            Log.d(TAG, "Segmenter not ready, setting up listener")
            segmenter.listener = this
        } else {
            Log.d(TAG, "Segmenter ready: let's process arguments")
            processFromArguments()
        }
    }

    private fun updateTextSize(increment: Int) {
        Log.d(TAG, "updateTextSize to $increment")
        val textSize = (appConfig.readerTextSize + increment).coerceAtLeast(10)

        if (textSize <= 10) {
            Toast.makeText(context, "Smaller text available", Toast.LENGTH_LONG).show()
        }

        appConfig.readerTextSize = textSize
    }

    private fun processFromArguments() {
        Log.d(TAG, "processFromArguments")

        if (arguments == null) return

        val imageUri = requireArguments().getString("imageUri") ?: ""
        val text = requireArguments().getString("preText") ?: ""
        if (imageUri != "") {
            Log.d(TAG, "processFromArguments: image was provided")

            viewModel.text.value = text

            if (text == "")
                viewModel.resetText()

            viewModel.recognizeText{ InputImage.fromFilePath(requireContext(), imageUri.toUri()) }
            requireArguments().putString("imageUri", "") // Consume condition
        } else if (text != "") {
            Log.d(TAG, "processFromArguments: text was provided")

            viewModel.text.value = text
        } else if (viewModel.text.value?.isEmpty() == true) { // text is empty
            Toast.makeText(requireContext(), "Oops - nothing to display", Toast.LENGTH_LONG).show()
        }
    }

    fun onWordClick(hanzi: String) {
        Log.d(TAG, "onWordClick $hanzi")

        viewModel.fetchWordForDisplay(hanzi, ::showSelectedWord)

        Log.d(TAG, "Augmenting Consulted Count for $hanzi by 1, if word exists")
        viewModel.augmentWordFrequencyConsulted(hanzi)
    }

    private fun showSelectedWord(simplified: String, annotatedWord: AnnotatedChineseWord?) {
        // Update the UI with the result
        if (annotatedWord == null) {
            Utils.copyToClipBoard(requireContext(), simplified)

            val message = requireContext().getString(R.string.ocr_display_word_not_found, simplified)
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()

            Utils.logAnalyticsEvent(Utils.ANALYTICS_EVENTS.OCR_WORD_NOTFOUND)
        } else {
            viewModel.clickedWords.postValue(
                viewModel.clickedWords.value.orEmpty()
                        + (simplified to annotatedWord.word?.pinyins.toString()))

            Utils.populateDictionaryEntryView(
                viewBinding.ocrDisplayDefinition, annotatedWord,
                findNavController(),
                { }
            )
            viewBinding.ocrDisplayDefinition.root.visibility = View.VISIBLE
            viewModel.selectedWord = simplified

            Utils.logAnalyticsEvent(Utils.ANALYTICS_EVENTS.OCR_WORD_FOUND)
        }
    }

    fun onTextAnalysisFailure(e: Error) {
        Log.d(TAG, "onTextAnalysisFailure: $e")

        Toast.makeText(context, getString(R.string.ocr_display_analysis_failure), Toast.LENGTH_LONG).show()

        Utils.logAnalyticsError(
            "OCR_DISPLAY",
            "TextAnalysisFailed",
            e.message ?: ""
        )
    }

    override fun onIsSegmenterReady() {
        Log.d(TAG, "onIsSegmenterReady")
        processFromArguments()
    }

    companion object {
        private const val TAG = "DisplayOCRFragment"

        private const val WORD_SEPARATOR = "·"
    }
}