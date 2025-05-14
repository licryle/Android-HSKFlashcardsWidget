package fr.berliat.hskwidget.ui.wordlist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import fr.berliat.hskwidget.R
import fr.berliat.hskwidget.data.model.WordListWithWords
import fr.berliat.hskwidget.databinding.FragmentWordlistDialogSelectItemBinding
import fr.berliat.hskwidget.databinding.FragmentWordlistDialogSelectListsBinding
import fr.berliat.hskwidget.ui.utils.AnkiIntegrationDelegate
import kotlinx.coroutines.launch

class WordListSelectionDialog : DialogFragment() {
    private lateinit var binding: FragmentWordlistDialogSelectListsBinding
    private lateinit var viewModel: WordListViewModel
    private lateinit var adapter: WordListSelectionAdapter
    private lateinit var ankiDelegate: AnkiIntegrationDelegate
    private var wordId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.Theme_HSKFlashCardsWidget_Dialog)

        ankiDelegate = AnkiIntegrationDelegate(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentWordlistDialogSelectListsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        wordId = arguments?.getString(ARG_WORD_ID)
        if (wordId == null) {
            dismiss()
            return
        }

        viewModel = WordListViewModel(requireContext(), ankiDelegate.wordListRepo)

        setupRecyclerView()
        setupButtons()
        loadData()
    }

    private fun setupRecyclerView() {
        adapter = WordListSelectionAdapter()
        binding.wordListRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@WordListSelectionDialog.adapter
        }
    }

    private fun setupButtons() {
        binding.cancelButton.setOnClickListener { dismiss() }
        
        binding.newListButton.setOnClickListener {
            showCreateListDialog()
        }
        
        binding.saveButton.setOnClickListener {
            saveListAssociations()
        }
    }

    private fun loadData() {
        viewLifecycleOwner.lifecycleScope.launch {
            // Load current word lists for this word
            viewModel.getWordListsForWord(wordId!!).collect { currentLists ->
                // Load all word lists
                viewModel.userWordLists().collect { allLists ->
                    adapter.submitList(allLists)
                    adapter.setSelectedLists(currentLists.map { it.wordList.id })
                }
            }
        }
    }

    private fun showCreateListDialog() {
        val dialog = WordListReNameDialog()
        dialog.show(childFragmentManager, "CreateListDialog")
    }

    private fun saveListAssociations() {
        val selectedListIds = adapter.getSelectedListIds()
        viewModel.updateWordListAssociations(wordId!!, selectedListIds) { error ->
            if (error == null) {
                Toast.makeText(context, R.string.wordlist_lists_updated_toast, Toast.LENGTH_SHORT)
                    .show()
                dismiss()
            } else {
                Toast.makeText(
                    context,
                    "Error updating lists: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    companion object {
        private const val ARG_WORD_ID = "word_id"

        fun newInstance(wordId: String): WordListSelectionDialog {
            return WordListSelectionDialog().apply {
                arguments = Bundle().apply {
                    putString(ARG_WORD_ID, wordId)
                }
            }
        }
    }
}

class WordListSelectionAdapter : RecyclerView.Adapter<WordListSelectionAdapter.ViewHolder>() {
    private var items = listOf<WordListWithWords>()
    private var selectedListIds = setOf<Long>()

    fun submitList(newItems: List<WordListWithWords>) {
        items = newItems
        notifyDataSetChanged()
    }

    fun setSelectedLists(listIds: List<Long>) {
        selectedListIds = listIds.toSet()
        notifyDataSetChanged()
    }

    fun getSelectedListIds(): List<Long> {
        return selectedListIds.toList()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = FragmentWordlistDialogSelectItemBinding.inflate(
            LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    inner class ViewHolder(private val binding: FragmentWordlistDialogSelectItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(wordList: WordListWithWords) {
            binding.wordListCheckBox.isChecked = wordList.wordList.id in selectedListIds
            binding.wordlistName.text = wordList.wordList.name
            binding.wordlistWordcount.text = itemView.context.getString(
                R.string.wordlist_word_count, wordList.words.size)

            binding.wordListRow.setOnClickListener {
                binding.wordListCheckBox.performClick()
            }

            binding.wordListCheckBox.setOnCheckedChangeListener { _, isChecked ->
                selectedListIds = if (isChecked) {
                    selectedListIds + wordList.wordList.id
                } else {
                    selectedListIds - wordList.wordList.id
                }
            }
        }
    }
} 