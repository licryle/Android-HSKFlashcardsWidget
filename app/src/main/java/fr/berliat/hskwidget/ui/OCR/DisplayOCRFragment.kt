package fr.berliat.hskwidget.ui.OCR

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import fr.berliat.hsktextviews.views.HSKTextView
import fr.berliat.hsktextviews.views.HSKWordView
import fr.berliat.hskwidget.R

import fr.berliat.hskwidget.data.dao.AnnotatedChineseWord
import fr.berliat.hskwidget.data.repo.ChineseWordFrequencyRepo
import fr.berliat.hskwidget.data.store.AppPreferencesStore
import fr.berliat.hskwidget.data.store.ChineseWordsDatabase
import fr.berliat.hskwidget.databinding.FragmentOcrDisplayBinding
import fr.berliat.hskwidget.domain.SharedViewModel
import fr.berliat.hskwidget.domain.Utils
import kotlinx.coroutines.launch
import androidx.core.net.toUri
import fr.berliat.hskwidget.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DisplayOCRFragment : Fragment(), HSKTextView.HSKTextListener, HSKTextView.HSKTextSegmenterListener {
    private lateinit var viewBinding: FragmentOcrDisplayBinding
    private lateinit var segmenter: HSKTextView.HSKTextSegmenter
    private lateinit var viewModel: DisplayOCRViewModel

    private suspend fun frequencyWordsRepo(): ChineseWordFrequencyRepo {
        val db = ChineseWordsDatabase.getInstance(requireContext())
        return ChineseWordFrequencyRepo(
            db.chineseWordFrequencyDAO(),
            db.annotatedChineseWordDAO()
        )
    }

    private var isProcessing = false

    private lateinit var appConfig: AppPreferencesStore

    private val DEFAULT_WORD_SEPARATOR = "·"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        segmenter = SharedViewModel.getInstance(this).segmenter
        viewModel = ViewModelProvider(this)[DisplayOCRViewModel::class.java]

        if (requireActivity().javaClass.simpleName == "MainActivity") {
            (requireActivity() as MainActivity).setOCRReminderVisible()
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

        toggleWordSeparator(appConfig.readerSeparateWords)
        viewBinding.ocrDisplayText.listener = this

        viewBinding.ocrDisplayConfBigger.setOnClickListener { updateTextSize(2) }
        viewBinding.ocrDisplayConfSmaller.setOnClickListener { updateTextSize(-2) }

        viewBinding.ocrDisplaySeparator.isChecked = appConfig.readerSeparateWords
        viewBinding.ocrDisplaySeparator.setOnClickListener {
            appConfig.readerSeparateWords = viewBinding.ocrDisplaySeparator.isChecked
            toggleWordSeparator(viewBinding.ocrDisplaySeparator.isChecked)
        }

        setupSegmenter()

        return viewBinding.root // Return the root view of the binding
    }

    override fun onPause() {
        super.onPause()
        // Save the scroll position
        viewModel.text = viewBinding.ocrDisplayText.text
        viewModel.scrollPosition = (viewBinding.ocrDisplayText.layoutManager as FlexboxLayoutManager).findFirstVisibleItemPosition()
    }

    override fun onResume() {
        super.onResume()

        Utils.logAnalyticsScreenView(requireContext(), "DisplayOCR")
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
            DEFAULT_WORD_SEPARATOR
        else
            ""
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
        if (imageUri != "") {
            Log.d(TAG, "processFromArguments: image was provided")
            viewModel.resetText()
            viewBinding.ocrDisplayText.text = arguments?.getString("preText") ?: ""

            toggleProcessing(true)
            viewModel.scrollPosition = 0 // new text, new scroll position

            requireArguments().putString("imageUri", "") // consume condition
            recognizeText(imageUri.toUri())
        } else if (viewModel.text != null) {
            Log.d(TAG, "processFromArguments: text was provided")

            toggleProcessing(true)

            // Add Global Layout Listener
            val globalLayoutListener = object : ViewTreeObserver.OnPreDrawListener {
                override fun onPreDraw() : Boolean {
                    // Restore scroll position
                    if (viewBinding.ocrDisplayText.adapter?.itemCount!! > viewModel.scrollPosition) {
                        viewBinding.ocrDisplayText.scrollToPosition(viewModel.scrollPosition)
                        // Remove the listener to prevent multiple calls
                        viewBinding.ocrDisplayText.viewTreeObserver.removeOnPreDrawListener(this)
                    }

                    return true // Allow the drawing to proceed
                }
            }
            viewBinding.ocrDisplayText.viewTreeObserver.addOnPreDrawListener(globalLayoutListener)

            viewBinding.ocrDisplayText.clickedWords = viewModel.clickedWords
            viewBinding.ocrDisplayText.text = viewModel.text!!
            viewModel.text = null // consume condition

            if (viewModel.selectedWord != null) {
                fetchWordForDisplay(viewModel.selectedWord!!, ::showSelectedWord)
            }
        } else {
            viewBinding.ocrDisplayText.text = ""
            Toast.makeText(requireContext(), "Oops - nothing to display", Toast.LENGTH_LONG).show()
        }
    }

    @OptIn(ExperimentalGetImage::class)
    private fun recognizeText(uri: Uri) {
        Log.d(TAG, "recognizeText starting")
        // Convert the Uri to InputImage for OCR
        val image = InputImage.fromFilePath(requireContext(), uri)

        val options = ChineseTextRecognizerOptions.Builder()
            .build()

        val recognizer: TextRecognizer = TextRecognition.getClient(options)

        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                processTextRecognitionResult(visionText)
                Log.d(TAG, "Recognized text: ${visionText.text}")
            }
            .addOnFailureListener { e ->
                toggleProcessing(false)
                Toast.makeText(requireContext(), "Text recognition failed", Toast.LENGTH_LONG).show()
                Log.e(TAG, "Text recognition failed: ", e)
                e.printStackTrace()
            }
    }

    private fun processTextRecognitionResult(texts: Text) {
        Log.d(TAG, "processTextRecognitionResult")
        val blocks: List<Text.TextBlock> = texts.textBlocks
        if (blocks.isEmpty()) {
            toggleProcessing(false)
            Toast.makeText(requireContext(), "No text found", Toast.LENGTH_SHORT).show()
            return
        }

        var concatText = ""
        for (i in blocks.indices) {
            val lines: List<Text.Line> = blocks[i].lines
            for (j in lines.indices) {
                val elements: List<Text.Element> = lines[j].elements
                for (k in elements.indices) {
                    Log.d(TAG, elements[k].text)
                    concatText += elements[k].text
                }
                Log.d(TAG, "END OF LINE")
                concatText += "\n\n"
            }
            Log.d(TAG, "END OF BLOCK")
        }

        Log.i(TAG, "Text recognition extracted, moving to display fragment: \n$concatText")

        val incremental = (arguments?.getBoolean("incrementalOCR") ?: "") == true
        if (incremental)
            viewBinding.ocrDisplayText.text += concatText
        else
            viewBinding.ocrDisplayText.text = concatText
    }

    private suspend fun fetchWord(hanzi: String): AnnotatedChineseWord? {
        Log.d("DisplayOCRFragment", "Searching for $hanzi")
        val db = ChineseWordsDatabase.getInstance(requireContext())
        val dao = db.annotatedChineseWordDAO()
        try {
            val word = dao.getFromSimplified(hanzi)
            Log.d("DictionarySearchFragment", "Search returned for $hanzi")

            return word
        } catch (e: Exception) {
            // Code for handling the exception
            Log.e("DictionarySearchFragment", "$e")
        }

        return null
    }

    private fun toggleProcessing(itIs: Boolean) {
        if (itIs) {
            viewBinding.ocrDisplayLoading.visibility = View.VISIBLE
        } else {
            viewBinding.ocrDisplayLoading.visibility = View.GONE
        }

        isProcessing = itIs
    }

    private fun fetchWordForDisplay(simplified: String, callback: (String, AnnotatedChineseWord?) -> Unit) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            // Switch to the IO dispatcher to perform background work
            val result = fetchWord(simplified)

            withContext(Dispatchers.Main) {
                callback(simplified, result)
            }
        }
    }

    override fun onWordClick(word: HSKWordView) {
        Log.d(TAG, "onWordClick ${word.hanziText}")

        fetchWordForDisplay(word.hanziText, ::showSelectedWord)

        lifecycle.coroutineScope.launch(Dispatchers.IO) {
            Log.d(TAG, "Augmenting Consulted Count for ${word.hanziText} by 1, if word exists")
            frequencyWordsRepo().incrementConsulted(word.hanziText)
        }
    }

    private fun showSelectedWord(simplified: String, annotatedWord: AnnotatedChineseWord?) {
        // Update the UI with the result
        if (annotatedWord == null) {
            viewModel.clickedWords[simplified] = ""
            Utils.copyToClipBoard(requireContext(), simplified)

            val message = requireContext().getString(R.string.ocr_display_word_not_found, simplified)
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        } else {
            viewModel.clickedWords[simplified] = annotatedWord.word?.pinyins.toString()

            Utils.populateDictionaryEntryView(
                viewBinding.ocrDisplayDefinition, annotatedWord,
                findNavController()
            )
            viewBinding.ocrDisplayDefinition.root.visibility = View.VISIBLE
            viewModel.selectedWord = simplified
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
    }

    override fun onTextAnalysisSuccess() {
        toggleProcessing(false)

        lifecycle.coroutineScope.launch(Dispatchers.IO) {
            val wordFreq = viewBinding.ocrDisplayText.wordsFrequency
            Log.d(TAG, "Augmenting Appeared Count for ${wordFreq.size} words (if they exist): $wordFreq")
            frequencyWordsRepo().incrementAppeared(wordFreq)
        }
    }

    override fun onIsSegmenterReady() {
        Log.d(TAG, "onIsSegmenterReady")
        processFromArguments()
    }

    companion object {
        private const val TAG = "DisplayOCRFragment"
    }
}