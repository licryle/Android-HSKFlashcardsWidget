package fr.berliat.hskwidget.ui.dictionary

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.google.android.material.materialswitch.MaterialSwitch
import fr.berliat.hsktextviews.views.HSKWordView
import fr.berliat.hskwidget.R
import fr.berliat.hskwidget.data.dao.AnnotatedChineseWord
import fr.berliat.hskwidget.data.model.ChineseWordAnnotation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date

class AnnotateFragment: Fragment() {
    private lateinit var simplifiedWord: String
    private lateinit var annotatedWord: AnnotatedChineseWord
    private lateinit var viewModel: AnnotateViewModel
    private lateinit var view : View

    private lateinit var chineseTextView: HSKWordView
    private lateinit var definitionTextText: TextView
    private lateinit var notesEditText: EditText
    private lateinit var classTypeSpinner: Spinner
    private lateinit var classLevelSpinner: Spinner
    private lateinit var themesEditText: EditText
    private lateinit var isExamSwitch: MaterialSwitch

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setActionBarTitle(getString(R.string.menu_annotate))
    }

    private fun setActionBarTitle(title: String) {
        (activity as AppCompatActivity).supportActionBar?.title = title
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        view = inflater.inflate(R.layout.fragment_annotation_edit, container, false)

        val factory = AnnotateViewModelFactory(requireContext())
        viewModel = ViewModelProvider(this, factory).get(AnnotateViewModel::class.java)

        chineseTextView = view.findViewById(R.id.annotation_edit_chinese)
        notesEditText = view.findViewById(R.id.annotation_edit_notes)
        definitionTextText = view.findViewById(R.id.annotation_edit_definition)
        classTypeSpinner = view.findViewById(R.id.annotation_edit_class_type)
        classLevelSpinner = view.findViewById(R.id.annotation_edit_class_level)
        themesEditText = view.findViewById(R.id.annotation_edit_themes)
        isExamSwitch = view.findViewById(R.id.annotation_edit_is_exam)


        simplifiedWord = arguments?.getString("simplifiedWord") ?: ""
        annotatedWord = AnnotatedChineseWord.getBlank(simplifiedWord)

        if (simplifiedWord != "") {
            GlobalScope.launch {
                // Switch to the IO dispatcher to perform background work
                annotatedWord = withContext(Dispatchers.IO) {
                    viewModel.getAnnotatedChineseWord(simplifiedWord)!! // Checked just below
                }
                // Switch back to the main thread to update UI
                withContext(Dispatchers.Main) {
                    if (!annotatedWord.hasAnnotation()) { // failure or new word
                        annotatedWord = AnnotatedChineseWord(
                            annotatedWord.word,
                            ChineseWordAnnotation.getBlank(simplifiedWord)
                        )
                    }
                    updateUI()
                }
            }
        }

        return updateUI()
    }

    private fun updateUI(): View {
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
        classTypeSpinner.adapter = classTypeAdapter

        // Populate the ClassLevel Spinner programmatically
        val classLevels = ChineseWordAnnotation.ClassLevel.entries.map { it.lvl }
        val classLevelAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            classLevels
        )
        classLevelAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        classLevelSpinner.adapter = classLevelAdapter

        // Update UI with ChineseWord fields
        chineseTextView.hanziText = annotatedWord.word?.simplified.toString()
        chineseTextView.pinyinText = annotatedWord.word?.pinyins.toString()

        // Populate fields from ChineseWordAnnotation
        notesEditText.setText(annotatedWord.annotation?.notes)
        classTypeSpinner.setSelection(annotatedWord.annotation?.classType?.ordinal ?: 0)
        classLevelSpinner.setSelection(annotatedWord.annotation?.level?.ordinal ?: 0)
        themesEditText.setText(annotatedWord.annotation?.themes)
        isExamSwitch.isChecked = annotatedWord.annotation?.isExam ?: false

        view.findViewById<Button>(R.id.annotation_edit_save).setOnClickListener(
            { onSaveClick() })

        val delBtn = view.findViewById<Button>(R.id.annotation_edit_delete)
        if (annotatedWord.hasAnnotation()) {
            delBtn.setOnClickListener({ showDeleteConfirmationDialog() })
        } else {
            delBtn.visibility = View.GONE
        }

        return view
    }

    private fun handleIOResult(action: ACTION, e: Exception?) {
        val msg_res : Int = when {
            action == ACTION.DELETE && e == null -> R.string.annotation_edit_delete_success
            action == ACTION.DELETE && e != null -> R.string.annotation_edit_delete_failure
            action == ACTION.UPDATE && e == null -> R.string.annotation_edit_save_success
            action == ACTION.UPDATE && e != null -> R.string.annotation_edit_save_failure
            else -> 0 // We'll crash
        }

        if (e == null) {
            Toast.makeText(context, getString(msg_res, simplifiedWord), Toast.LENGTH_LONG).show()
            findNavController().popBackStack()
        } else {
            Toast.makeText(context, getString(msg_res, simplifiedWord, e), Toast.LENGTH_LONG).show()
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
        var firstSeen = annotatedWord.annotation?.firstSeen
        if (firstSeen == null)
            firstSeen = Date()

        val updatedAnnotation = ChineseWordAnnotation(
            simplified = simplifiedWord,
            pinyins = null,  // Assume pinyins are handled elsewhere
            notes = notesEditText.text.toString(),
            classType = ChineseWordAnnotation.ClassType.entries[classTypeSpinner.selectedItemPosition],
            level = ChineseWordAnnotation.ClassLevel.entries[classLevelSpinner.selectedItemPosition],
            themes = themesEditText.text.toString(),
            firstSeen = firstSeen,  // Handle date logic
            isExam = isExamSwitch.isChecked
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
