package fr.berliat.hskwidget.ui.OCR

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.google.android.flexbox.FlexboxLayoutManager
import fr.berliat.hsktextviews.views.HSKTextView
import fr.berliat.hsktextviews.views.HSKWordView
import fr.berliat.hskwidget.R

import fr.berliat.hskwidget.data.model.AnnotatedChineseWord
import fr.berliat.hskwidget.data.store.AppPreferencesStore
import fr.berliat.hskwidget.databinding.FragmentOcrDisplayBinding
import fr.berliat.hskwidget.domain.Utils
import androidx.core.net.toUri
import fr.berliat.hsktextviews.views.HSKWordsAdapter.ShowPinyins
import fr.berliat.hskwidget.MainActivity
import fr.berliat.hskwidget.domain.SharedViewModel

class DisplayOCRFragment : Fragment(), HSKTextView.HSKTextListener, HSKTextView.HSKTextSegmenterListener {
    private lateinit var viewBinding: FragmentOcrDisplayBinding
    private lateinit var segmenter: HSKTextView.HSKTextSegmenter
    private lateinit var viewModel: DisplayOCRViewModel

    private lateinit var appConfig: AppPreferencesStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        segmenter = SharedViewModel.getInstance(this).segmenter

        val mainApp = requireActivity() as MainActivity
        val factory = DisplayOCRViewModelFactory(mainApp)
        viewModel = ViewModelProvider(this, factory)[DisplayOCRViewModel::class.java]

        if (requireActivity().javaClass.simpleName == "MainActivity") {
            mainApp.setOCRReminderVisible()
        }
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
                CaptureImageFragmentDirections.processOCR(viewBinding.ocrDisplayText.text)
            findNavController().navigate(action)
        }

        viewBinding.ocrDisplayText.segmenter = segmenter
        viewBinding.ocrDisplayText.hanziTextSize = appConfig.readerTextSize

        viewBinding.ocrDisplayText.listener = this

        viewBinding.ocrDisplayConfBigger.setOnClickListener { updateTextSize(2) }
        viewBinding.ocrDisplayConfSmaller.setOnClickListener { updateTextSize(-2) }

        viewBinding.ocrDisplaySeparator.isChecked = appConfig.readerSeparateWords
        toggleWordSeparator(appConfig.readerSeparateWords)
        viewBinding.ocrDisplaySeparator.setOnClickListener {
            appConfig.readerSeparateWords = viewBinding.ocrDisplaySeparator.isChecked
            toggleWordSeparator(viewBinding.ocrDisplaySeparator.isChecked)
        }

        viewBinding.ocrDisplayPinyins.isChecked = appConfig.readerShowAllPinyins
        toggleShowPinyins(appConfig.readerShowAllPinyins)
        viewBinding.ocrDisplayPinyins.setOnClickListener {
            appConfig.readerShowAllPinyins = viewBinding.ocrDisplayPinyins.isChecked
            toggleShowPinyins(viewBinding.ocrDisplayPinyins.isChecked)
        }

        viewModel.isProcessing.observe(viewLifecycleOwner) { value ->
            toggleProcessing(value)
        }

        viewModel.text.observe(viewLifecycleOwner) { value ->
            viewBinding.ocrDisplayText.text = value
        }

        viewModel.toastEvent.observe(viewLifecycleOwner) { value ->
            if (value.first.isNotEmpty()) {
                Toast.makeText(requireContext(), value.first, value.second).show()
            }
        }

        setupSegmenter()

        return viewBinding.root // Return the root view of the binding
    }

    override fun onPause() {
        super.onPause()
        // Save the scroll position
        viewModel.scrollPosition = (viewBinding.ocrDisplayText.layoutManager as FlexboxLayoutManager).findFirstVisibleItemPosition()
    }

    override fun onResume() {
        super.onResume()

        toggleProcessing(true)
        viewBinding.ocrDisplayText.text = viewModel.text.value ?: ""

        if (viewModel.text.value?.isNotEmpty() == true) {
            // Add Global Layout Listener
            val globalLayoutListener = object : ViewTreeObserver.OnPreDrawListener {
                override fun onPreDraw(): Boolean {
                    // Restore scroll position
                    if (viewBinding.ocrDisplayText.adapter?.itemCount!! > viewModel.scrollPosition) {
                        val position = viewModel.scrollPosition.coerceAtMost(
                            (viewBinding.ocrDisplayText.adapter?.itemCount ?: 0) - 1
                        )
                        viewBinding.ocrDisplayText.scrollToPosition(position)
                        // Remove the listener to prevent multiple calls
                        viewBinding.ocrDisplayText.viewTreeObserver.removeOnPreDrawListener(this)
                    }

                    return true // Allow the drawing to proceed
                }
            }
            viewBinding.ocrDisplayText.viewTreeObserver.addOnPreDrawListener(globalLayoutListener)

            viewBinding.ocrDisplayText.clickedWords = viewModel.clickedWords
            if (viewModel.selectedWord != null) {
                viewModel.fetchWordForDisplay(viewModel.selectedWord!!, ::showSelectedWord)
            }
        }

        Utils.logAnalyticsScreenView("DisplayOCR")
    }

    private fun setupSegmenter() {
        toggleProcessing(true)

        if (! segmenter.isReady()) {
            Log.d(TAG, "Segmenter not ready, setting up listener")
            segmenter.listener = this
        } else {
            Log.d(TAG, "Segmenter ready: let's process arguments")
            processFromArguments()
        }
    }

    private fun toggleWordSeparator(separate: Boolean) {
        viewBinding.ocrDisplayText.wordSeparator = if (separate)
            WORD_SEPARATOR
        else
            ""
    }

    private fun toggleShowPinyins(showThem: Boolean) {
        viewBinding.ocrDisplayText.showPinyins = if (showThem)
            ShowPinyins.ALL
        else
            ShowPinyins.CLICKED
    }

    private fun updateTextSize(increment: Int) {
        Log.d(TAG, "updateTextSize to $increment")
        val textSize = (appConfig.readerTextSize + increment).coerceAtLeast(10)

        if (textSize <= 10) {
            Toast.makeText(context, "Smaller text available", Toast.LENGTH_LONG).show()
        }

        appConfig.readerTextSize = textSize
        viewBinding.ocrDisplayText.hanziTextSize = textSize
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

            viewModel.recognizeText(imageUri.toUri())
            requireArguments().putString("imageUri", "") // Consume condition
        } else if (text != "") {
            Log.d(TAG, "processFromArguments: text was provided")

            viewModel.text.value = text
        } else if (viewModel.text.value?.isEmpty() == true) { // text is empty
            Toast.makeText(requireContext(), "Oops - nothing to display", Toast.LENGTH_LONG).show()
        }
    }

    private fun toggleProcessing(itIs: Boolean) {
        if (itIs) {
            viewBinding.ocrDisplayLoading.visibility = View.VISIBLE
        } else {
            viewBinding.ocrDisplayLoading.visibility = View.GONE
        }
    }

    override fun onWordClick(word: HSKWordView) {
        Log.d(TAG, "onWordClick ${word.hanziText}")

        viewModel.fetchWordForDisplay(word.hanziText, ::showSelectedWord)

        Log.d(TAG, "Augmenting Consulted Count for ${word.hanziText} by 1, if word exists")
        viewModel.augmentWordFrequencyConsulted(word.hanziText)
    }

    private fun showSelectedWord(simplified: String, annotatedWord: AnnotatedChineseWord?) {
        // Update the UI with the result
        if (annotatedWord == null) {
            Utils.copyToClipBoard(requireContext(), simplified)

            val message = requireContext().getString(R.string.ocr_display_word_not_found, simplified)
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()

            Utils.logAnalyticsEvent(Utils.ANALYTICS_EVENTS.OCR_WORD_NOTFOUND)
        } else {
            viewModel.clickedWords[simplified] = annotatedWord.word?.pinyins.toString()

            Utils.populateDictionaryEntryView(
                viewBinding.ocrDisplayDefinition, annotatedWord,
                findNavController(),
                { }
            )
            viewBinding.ocrDisplayDefinition.root.visibility = View.VISIBLE
            viewModel.selectedWord = simplified

            Utils.logAnalyticsEvent(Utils.ANALYTICS_EVENTS.OCR_WORD_FOUND)
        }

        viewBinding.ocrDisplayText.clickedWords = viewModel.clickedWords
    }

    override fun onTextAnalysisStart() {
        toggleProcessing(true)
    }

    override fun onTextAnalysisFailure(e: Error) {
        Log.d(TAG, "onTextAnalysisFailure: $e")

        toggleProcessing(false)
        Toast.makeText(context, getString(R.string.ocr_display_analysis_failure), Toast.LENGTH_LONG).show()

        Utils.logAnalyticsError(
            "OCR_DISPLAY",
            "TextAnalysisFailed",
            e.message ?: ""
        )
    }

    override fun onTextAnalysisSuccess() {
        toggleProcessing(false)

        val wordFreq = viewBinding.ocrDisplayText.wordsFrequency
        viewModel.augmentWordFrequencyAppeared(wordFreq)
        Log.d(TAG, "Augmenting Appeared Count for ${wordFreq.size} words (if they exist): $wordFreq")
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