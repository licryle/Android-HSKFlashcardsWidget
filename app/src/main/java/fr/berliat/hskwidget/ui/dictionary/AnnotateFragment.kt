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
import androidx.navigation.fragment.findNavController
import fr.berliat.hskwidget.R
import fr.berliat.hskwidget.data.model.AnnotatedChineseWord
import fr.berliat.hskwidget.data.model.ChineseWord
import fr.berliat.hskwidget.data.model.ChineseWordAnnotation
import fr.berliat.hskwidget.data.store.AppPreferencesStore
import fr.berliat.hskwidget.databinding.FragmentAnnotationEditBinding
import fr.berliat.hskwidget.domain.Utils
import fr.berliat.hskwidget.domain.Utils.Companion.copyToClipBoard
import fr.berliat.hskwidget.domain.Utils.Companion.playWordInBackground
import fr.berliat.hskwidget.ui.utils.AnkiDelegate
import java.time.Instant
import java.util.Date


class AnnotateFragment: Fragment() {
    private lateinit var binding: FragmentAnnotationEditBinding
    private lateinit var annotateViewModel: AnnotateViewModel
    private lateinit var ankiDelegate: AnkiDelegate

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setActionBarTitle(getString(R.string.menu_annotate))

        ankiDelegate = AnkiDelegate(this)
    }

    private fun setActionBarTitle(title: String) {
        (activity as AppCompatActivity).supportActionBar?.title = title
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentAnnotationEditBinding.inflate(inflater, container, false)

        annotateViewModel = AnnotateViewModel(requireContext(), ankiDelegate.wordListRepo)
        annotateViewModel.annotatedWord.observe(viewLifecycleOwner) { word ->
            updateUI(word)
        }
        annotateViewModel.fetchAnnotatedWord(arguments)

        Utils.hideKeyboard(requireContext(), binding.root)

        return binding.root
    }

    override fun onResume() {
        super.onResume()

        Utils.logAnalyticsScreenView("Annotate")
    }

    private fun updateUI(annotatedWord: AnnotatedChineseWord) {
        // Populate the ClassType Spinner programmatically
        val classTypes =
            ChineseWordAnnotation.ClassType.entries.map { it.type }  // Convert enum to list of strings
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
        binding.annotationEditChinese.hanziText = annotatedWord.word?.simplified.toString()
        binding.annotationEditChinese.pinyinText = annotatedWord.word?.pinyins.toString()

        binding.dictionaryItemHskLevel.text = annotatedWord.word?.hskLevel.toString()
        binding.dictionaryItemHskLevel.visibility =
            if (annotatedWord.word?.hskLevel == null || annotatedWord.word.hskLevel == ChineseWord.HSK_Level.NOT_HSK) {
                    View.INVISIBLE
            } else {
                View.VISIBLE
            }

        val prefStore = AppPreferencesStore(requireContext())
        // Populate fields from ChineseWordAnnotation
        binding.annotationEditNotes.setText(annotatedWord.annotation?.notes)
        if (annotatedWord.hasAnnotation()) {
            binding.annotationEditClassType.setSelection(annotatedWord.annotation!!.classType!!.ordinal)
            binding.annotationEditClassLevel.setSelection(annotatedWord.annotation.level!!.ordinal)
        } else {
            binding.annotationEditClassType.setSelection(prefStore.lastAnnotatedClassType.ordinal)
            binding.annotationEditClassLevel.setSelection(prefStore.lastAnnotatedClassLevel.ordinal)
        }
        binding.annotationEditThemes.setText(annotatedWord.annotation?.themes)
        binding.annotationEditIsExam.isChecked = annotatedWord.annotation?.isExam ?: false

        binding.dictionaryItemSpeak.setOnClickListener {
            playWordInBackground(requireContext(), annotatedWord.simplified)
        }
        binding.dictionaryItemCopy.setOnClickListener { copyToClipBoard(requireContext(), annotatedWord.simplified) }

        binding.annotationEditSave.setOnClickListener { onSaveClick() }

        if (annotatedWord.hasAnnotation()) {
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
            Toast.makeText(context, getString(msgRes, annotateViewModel.simplified), Toast.LENGTH_LONG).show()
            findNavController().popBackStack()
        } else {
            Toast.makeText(context, getString(msgRes, annotateViewModel.simplified, e), Toast.LENGTH_LONG).show()
        }
    }

    private fun showDeleteConfirmationDialog() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle(getString(R.string.annotation_edit_delete_confirm_title))
            .setMessage(getString(R.string.annotation_edit_delete_confirm_message))
            .setPositiveButton(getString(R.string.delete)) { dialog, _ ->
                // Handle positive action (e.g., save annotation)
                annotateViewModel.deleteAnnotation { err -> handleIOResult(ACTION.DELETE, err) }

                dialog.dismiss() // Dismiss the dialog
            }
            .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                // Handle negative action (e.g., cancel)
                dialog.dismiss() // Dismiss the dialog
            }


        val dialog = builder.create()
        dialog.show()
    }

    private fun onSaveClick() {            // Save the updated annotation fields
        var firstSeen = annotateViewModel.annotatedWord.value?.annotation?.firstSeen
        if (firstSeen == null)
            firstSeen = Date(Instant.now().toEpochMilli())

        val updatedAnnotation = ChineseWordAnnotation(
            simplified = annotateViewModel.simplified.trim(),
            pinyins = null,  // Assume pinyins are handled elsewhere
            notes = binding.annotationEditNotes.text.toString(),
            classType = ChineseWordAnnotation.ClassType.entries[binding.annotationEditClassType.selectedItemPosition],
            level = ChineseWordAnnotation.ClassLevel.entries[binding.annotationEditClassLevel.selectedItemPosition],
            themes = binding.annotationEditThemes.text.toString(),
            firstSeen = firstSeen,  // Handle date logic
            isExam = binding.annotationEditIsExam.isChecked
        )

        val annotatedWord = AnnotatedChineseWord(annotateViewModel.annotatedWord.value!!.word, updatedAnnotation)
        annotateViewModel.updateAnnotation(annotatedWord) { err -> handleIOResult(ACTION.UPDATE, err) }

        Utils.incrementConsultedWord(requireContext(), annotateViewModel.simplified)

        if (annotatedWord.hasAnnotation()) {
            AppPreferencesStore(requireContext()).lastAnnotatedClassType = updatedAnnotation.classType!!
            AppPreferencesStore(requireContext()).lastAnnotatedClassLevel = updatedAnnotation.level!!
        }
    }

    enum class ACTION {
        UPDATE,
        DELETE
    }
}
