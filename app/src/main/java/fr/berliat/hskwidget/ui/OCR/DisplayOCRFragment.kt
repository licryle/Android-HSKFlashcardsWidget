package fr.berliat.hskwidget.ui.OCR

import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import fr.berliat.hsktextviews.views.HSKTextView
import fr.berliat.hsktextviews.views.HSKWordView
import fr.berliat.hskwidget.R

import fr.berliat.hskwidget.data.dao.AnnotatedChineseWord
import fr.berliat.hskwidget.data.store.AppPreferencesStore
import fr.berliat.hskwidget.data.store.ChineseWordsDatabase
import fr.berliat.hskwidget.databinding.FragmentOcrDisplayBinding
import fr.berliat.hskwidget.domain.SharedViewModel
import fr.berliat.hskwidget.domain.Utils
import kotlinx.coroutines.launch

class DisplayOCRFragment : Fragment(), HSKTextView.HSKTextListener, HSKTextView.HSKTextSegmenterListener {
    private lateinit var viewBinding: FragmentOcrDisplayBinding
    private lateinit var segmenter: HSKTextView.HSKTextSegmenter

    private var isProcessing = false

    private lateinit var appConfig: AppPreferencesStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        segmenter = SharedViewModel.getInstance(this).segmenter
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        //if (SharedViewModel.getInstance(this).ocr_view_binding != null) {
        //    viewBinding = SharedViewModel.getInstance(this).ocr_view_binding!!
        //} else {
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

        //    SharedViewModel.getInstance(this).ocr_view_binding = viewBinding
        //}

        viewBinding.ocrDisplayConfBigger.setOnClickListener { updateTextSize(2) }
        viewBinding.ocrDisplayConfSmaller.setOnClickListener { updateTextSize(-2) }
        viewBinding.ocrDisplayText.listener = this

        setupSegmenter()

        return viewBinding.root // Return the root view of the binding
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
        viewBinding.ocrDisplayText.text = arguments?.getString("preText") ?: ""

        val imageUri = arguments?.getString("imageUri") ?: ""
        if (imageUri == "")
            // oopsie
            throw Exception("No imageURI passed to parse")

        toggleProcessing(true)
        recognizeText(Uri.parse(imageUri))
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
            }
            concatText += "\n\n"
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
            viewBinding.ocrDisplayAdd.visibility = View.GONE
            viewBinding.ocrDisplayLoading.visibility = View.VISIBLE
        } else {
            viewBinding.ocrDisplayAdd.visibility = View.VISIBLE
            viewBinding.ocrDisplayLoading.visibility = View.GONE
        }

        isProcessing = itIs
    }


    companion object {
        private const val TAG = "DisplayOCRFragment"
    }

    override fun onWordClick(word: HSKWordView) {
        Log.d(TAG, "onWordClick ${word.hanziText}")
        word.setBackgroundColor(Color.CYAN)

        viewLifecycleOwner.lifecycleScope.launch {
            // Switch to the IO dispatcher to perform background work
            val result = fetchWord(word.hanziText)

            // Update the UI with the result
            if (result == null)
                Toast.makeText(context, "Couldn't find ${word.hanziText}", Toast.LENGTH_LONG).show()
            else {
                word.pinyinText = result.word?.pinyins.toString()

                Utils.populateDictionaryEntryView(viewBinding.ocrDisplayDefinition,result,
                    findNavController())
                viewBinding.root.visibility = View.VISIBLE
            }
        }
    }

    override fun onTextAnalysisStart() {
        toggleProcessing(true)
    }

    override fun onTextAnalysisSuccess() {
        toggleProcessing(false)
    }

    override fun onTextAnalysisFailure(e: Error) {
        Log.d(TAG, "onTextAnalysisFailure: $e")

        toggleProcessing(false)
        Toast.makeText(context, getString(R.string.ocr_display_analysis_failure), Toast.LENGTH_LONG).show()
    }

    override fun onIsSegmenterReady() {
        Log.d(TAG, "onIsSegmenterReady")
        processFromArguments()
    }
}