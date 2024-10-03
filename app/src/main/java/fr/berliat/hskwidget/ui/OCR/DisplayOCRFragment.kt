package fr.berliat.hskwidget.ui.OCR

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import fr.berliat.hskwidget.R
import fr.berliat.hskwidget.data.dao.AnnotatedChineseWord
import fr.berliat.hskwidget.data.store.ChineseWordsDatabase

import fr.berliat.hskwidget.databinding.FragmentOcrDisplayBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class DisplayOCRFragment : Fragment() {
    private lateinit var viewBinding: FragmentOcrDisplayBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewBinding = FragmentOcrDisplayBinding.inflate(inflater, container, false) // Inflate here

        viewBinding.ocrDisplayAdd.setOnClickListener {
            val action = DisplayOCRFragmentDirections.incrementalOCR(true)
            findNavController().navigate(action)
        }

        val text = arguments?.getString("text") ?: ""
        val incremental = (arguments?.getBoolean("incrementalOCR") ?: "") == true

        if (incremental)
            viewBinding.ocrDisplayText.text += text
        else
            viewBinding.ocrDisplayText.text = text

        viewBinding.ocrDisplayText.setOnWordClickListener { word ->
            word.setBackgroundColor(Color.CYAN)
            GlobalScope.launch {
                // Switch to the IO dispatcher to perform background work
                val result = withContext(Dispatchers.IO) {
                    fetchWord(word.hanziText)
                }

                // Switch back to the main thread to update UI
                withContext(Dispatchers.Main) {
                    // Update the UI with the result
                    if (result == null)
                        Toast.makeText(context, "Couldn't find ${word.hanziText}", Toast.LENGTH_LONG)
                    else {
                        val vb = viewBinding.ocrDisplayDefinition
                        vb.dictionaryItemChinese.hanziText = result.simplified.toString()
                        vb.dictionaryItemChinese.pinyinText = result.word?.pinyins.toString()
                        vb.dictionaryItemDefinition.text =
                            result.word?.definition?.get(Locale.ENGLISH)?.toString() ?: ""

                        val view = requireView().findViewById<View>(R.id.ocr_display_definition)
                        view.visibility = View.VISIBLE
                    }
                }
            }
        }

        return viewBinding.root // Return the root view of the binding
    }

    private suspend fun fetchWord(hanzi: String): AnnotatedChineseWord? {
        Log.d("DisplayOCRFragment", "Searching for ${hanzi}")
        val db = ChineseWordsDatabase.getInstance(requireContext())
        val dao = db.annotatedChineseWordDAO()
        try {
            val word = dao.findWordFromSimplified(hanzi)
            Log.d("DictionarySearchFragment", "Search returned for ${hanzi}")

            return word
        } catch (e: Exception) {
            // Code for handling the exception
            Log.e("DictionarySearchFragment", "$e")
        }

        return null
    }
}