package fr.berliat.hskwidget.ui.dictionary

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import fr.berliat.hskwidget.R
import fr.berliat.hskwidget.data.dao.AnnotatedChineseWord
import fr.berliat.hskwidget.data.model.ChineseWord
import fr.berliat.hskwidget.data.model.ChineseWordAnnotation
import fr.berliat.hskwidget.databinding.FragmentAnnotationEditBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date

class AnnotateFragment: Fragment() {
    private lateinit var simplifiedWord: String
    private var annotatedWord: AnnotatedChineseWord? = null
    private lateinit var binding: FragmentAnnotationEditBinding
    private lateinit var viewModel: AnnotateViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setActionBarTitle(getString(R.string.menu_annotate))
    }

    private fun setActionBarTitle(title: String) {
        (activity as AppCompatActivity).supportActionBar?.title = title
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentAnnotationEditBinding.inflate(inflater, container, false)

        val factory = AnnotateViewModelFactory(requireContext())
        viewModel = ViewModelProvider(this, factory).get(AnnotateViewModel::class.java)

        simplifiedWord = arguments?.getString("simplifiedWord") ?: ""
        if (simplifiedWord == "") {
            annotatedWord = AnnotatedChineseWord.getBlank(simplifiedWord)
        } else {
            GlobalScope.launch {
                // Switch to the IO dispatcher to perform background work
                annotatedWord = withContext(Dispatchers.IO) {
                    viewModel.getAnnotatedChineseWord(simplifiedWord) // Checked just below
                }
                // Switch back to the main thread to update UI
                withContext(Dispatchers.Main) {
                    if (annotatedWord == null || annotatedWord?.hasAnnotation() == false) { // failure or new word
                        annotatedWord = AnnotatedChineseWord(
                            annotatedWord?.word ?: ChineseWord.getBlank(simplifiedWord),
                            ChineseWordAnnotation.getBlank(simplifiedWord)
                        )
                    }
                    updateUI()
                }
            }
        }

        return binding.root
    }

    private fun updateUI() {
        // Initialize ViewModel (you might want to use a ViewModelFactory)
        viewModel = ViewModelProvider(this).get(AnnotateViewModel::class.java)

        // Populate the ClassType Spinner programmatically
        val classTypes = ChineseWordAnnotation.ClassType.entries.map { it.type }  // Convert enum to list of strings
        val classTypeAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            classTypes
        )
        classTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.annotationEditClassType.adapter = classTypeAdapter

        // Populate the ClassLevel Spinner programmatically
        val classLevels = ChineseWordAnnotation.ClassLevel.entries.map { it.lvl }
        val classLevelAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            classLevels
        )
        classLevelAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.annotationEditClassLevel.adapter = classLevelAdapter

        // Update UI with ChineseWord fields
        binding.annotationEditChinese.hanziText = annotatedWord?.word?.simplified.toString()
        binding.annotationEditChinese.pinyinText = annotatedWord?.word?.pinyins.toString()

        // Populate fields from ChineseWordAnnotation
        binding.annotationEditNotes.setText(annotatedWord?.annotation?.notes)
        binding.annotationEditClassType.setSelection(annotatedWord?.annotation?.classType?.ordinal ?: 0)
        binding.annotationEditClassLevel.setSelection(annotatedWord?.annotation?.level?.ordinal ?: 0)
        binding.annotationEditThemes.setText(annotatedWord?.annotation?.themes)
        binding.annotationEditIsExam.isChecked = annotatedWord?.annotation?.isExam ?: false

        binding.annotationEditSave.setOnClickListener { onSaveClick() }

        if (annotatedWord?.hasAnnotation() == true) {
            binding.annotationEditDelete.setOnClickListener { showDeleteConfirmationDialog() }
        } else {
            binding.annotationEditDelete.visibility = View.GONE
        }
    }

    private fun handleIOResult(action: ACTION, e: Exception?) {
        val msgRes : Int = when {
            action == ACTION.DELETE && e == null -> R.string.annotation_edit_delete_success
            action == ACTION.DELETE && e != null -> R.string.annotation_edit_delete_failure
            action == ACTION.UPDATE && e == null -> R.string.annotation_edit_save_success
            action == ACTION.UPDATE && e != null -> R.string.annotation_edit_save_failure
            else -> 0 // We'll crash
        }

        if (e == null) {
            Toast.makeText(context, getString(msgRes, simplifiedWord), Toast.LENGTH_LONG).show()
            findNavController().popBackStack()
        } else {
            Toast.makeText(context, getString(msgRes, simplifiedWord, e), Toast.LENGTH_LONG).show()
        }
    }

    private fun showDeleteConfirmationDialog() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle(getString(R.string.annotation_edit_delete_confirm_title))
            .setMessage(getString(R.string.annotation_edit_delete_confirm_message))
            .setPositiveButton(getString(R.string.annotation_edit_delete_confirm_yes)) { dialog, which ->
                // Handle positive action (e.g., save annotation)
                GlobalScope.launch {
                    val err = viewModel.deleteAnnotation(simplifiedWord)

                    withContext(Dispatchers.Main) {
                        handleIOResult(ACTION.DELETE, err)
                    }
                }

                dialog.dismiss() // Dismiss the dialog
            }
            .setNegativeButton(getString(R.string.annotation_edit_delete_confirm_no)) { dialog, which ->
                // Handle negative action (e.g., cancel)
                dialog.dismiss() // Dismiss the dialog
            }

        val dialog = builder.create()
        dialog.show()
    }

    private fun onSaveClick() {            // Save the updated annotation fields
        var firstSeen = annotatedWord?.annotation?.firstSeen
        if (firstSeen == null)
            firstSeen = Date()

        val updatedAnnotation = ChineseWordAnnotation(
            simplified = simplifiedWord,
            pinyins = null,  // Assume pinyins are handled elsewhere
            notes = binding.annotationEditNotes.text.toString(),
            classType = ChineseWordAnnotation.ClassType.entries[binding.annotationEditClassType.selectedItemPosition],
            level = ChineseWordAnnotation.ClassLevel.entries[binding.annotationEditClassLevel.selectedItemPosition],
            themes = binding.annotationEditThemes.text.toString(),
            firstSeen = firstSeen,  // Handle date logic
            isExam = binding.annotationEditIsExam.isChecked
        )

        GlobalScope.launch {
            val err = viewModel.updateAnnotation(updatedAnnotation)

            withContext(Dispatchers.Main) {
                handleIOResult(ACTION.UPDATE, err)
            }
        }
    }

    enum class ACTION {
        DELETE,
        UPDATE
    }
}
