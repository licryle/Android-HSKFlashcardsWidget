package fr.berliat.hskwidget.ui.OCR

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController

import fr.berliat.hskwidget.databinding.FragmentOcrDisplayBinding

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

        var text = arguments?.getString("text") ?: ""
        val incremental = arguments?.getBoolean("incrementalOCR") ?: "" == true

        if (incremental)
            viewBinding.ocrDisplayText.text = viewBinding.ocrDisplayText.text.toString() + text
        else
            viewBinding.ocrDisplayText.text = text

        viewBinding.ocrDisplayText.setOnWordClickListener { word ->
            word.setBackgroundColor(Color.RED)
        }

        return viewBinding.root // Return the root view of the binding
    }
}