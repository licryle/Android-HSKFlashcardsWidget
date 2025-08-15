package fr.berliat.hskwidget.ui.wordlist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import fr.berliat.hskwidget.R
import fr.berliat.hskwidget.data.repo.WordListRepository
import fr.berliat.hskwidget.databinding.FragmentWordlistDialogCreateListBinding
import fr.berliat.hskwidget.domain.Utils

class WordListReNameDialog(
    private val onReNameResult: (success: Boolean, newName: String?) -> Unit) : DialogFragment() {
    private lateinit var binding: FragmentWordlistDialogCreateListBinding
    private lateinit var viewModel: WordListViewModel

    companion object {
        private const val ARG_LIST_ID = "list_id"
        private const val ARG_LIST_NAME = "list_name"

        fun newInstance(listId: Long?, listName: String?,
                        onReNameResult: (success: Boolean, newName: String?) -> Unit): WordListReNameDialog {
            val args = Bundle().apply {
                listId?.let { putLong(ARG_LIST_ID, it) }
                listName?.let { putString(ARG_LIST_NAME, it) }
            }
            return WordListReNameDialog(onReNameResult).apply {
                arguments = args
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.Theme_HSKFlashCardsWidget_Dialog)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentWordlistDialogCreateListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = WordListViewModel(WordListRepository(requireContext()))

        val listId = arguments?.getLong(ARG_LIST_ID)
        val existingName = arguments?.getString(ARG_LIST_NAME)

        if (listId != null && existingName != null) {
            binding.listNameInput.setText(existingName)
            binding.createButton.text = getString(R.string.wordlist_rename_button)
        }

        binding.cancelButton.setOnClickListener {
            dismiss()
            onReNameResult(false, null)
        }

        binding.createButton.setOnClickListener {
            val name = binding.listNameInput.text.toString().trim()
            if (name.isNotBlank()) {
                if (listId != null) {
                    viewModel.renameList(listId, name) { err ->
                        if (err == null) {
                            onReNameResult(true, name)
                            Utils.logAnalyticsEvent(requireContext(), Utils.ANALYTICS_EVENTS.LIST_RENAME)

                            dismiss()
                        } else {
                            Toast.makeText(context, getString(R.string.wordlist_list_name_dupe), Toast.LENGTH_LONG).show()
                        }
                    }
                } else {
                    viewModel.createList(name) { err ->
                        if (err == null) {
                            onReNameResult(true, name)

                            Utils.logAnalyticsEvent(requireContext(), Utils.ANALYTICS_EVENTS.LIST_CREATE)

                            dismiss()
                        } else {
                            Toast.makeText(context, getString(R.string.wordlist_list_name_dupe), Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
    }
}
