package fr.berliat.hskwidget.ui.wordlist

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import fr.berliat.hskwidget.R
import fr.berliat.hskwidget.data.model.WordList
import fr.berliat.hskwidget.data.model.WordListWithWords
import fr.berliat.hskwidget.ui.utils.AnkiDelegate
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class WordListFragment : Fragment() {
    private lateinit var viewModel: WordListViewModel
    private lateinit var adapter: WordListAdapter
    private lateinit var ankiDelegate: AnkiDelegate

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ankiDelegate = AnkiDelegate(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_wordlist, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = WordListViewModel(requireContext(), ankiDelegate.wordListRepo)

        val recyclerView = view.findViewById<RecyclerView>(R.id.wordListRecyclerView)
        adapter = WordListAdapter(
            requireContext(),
            onConsultClick = { list -> consultList(list) },
            onDeleteClick = { list -> showDeleteConfirmation(list) },
            onRenameClick = { list -> showRenameDialog(list) }
        )
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter

        view.findViewById<FloatingActionButton>(R.id.addListFab).setOnClickListener {
            showCreateListDialog()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.wordLists.collectLatest { lists ->
                adapter.submitList(lists)
            }
        }
    }

    private fun showRenameDialog(list: WordListWithWords) {
        WordListReNameDialog.newInstance(listId = list.wordList.id, listName = list.wordList.name)
            .show(requireFragmentManager(), "RenameListDialog")
    }

    private fun showCreateListDialog() {
        val dialog = WordListReNameDialog()
        dialog.show(childFragmentManager, "CreateListDialog")
    }

    private fun showDeleteConfirmation(list: WordListWithWords) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.wordlist_delete_button)
            .setMessage(getString(R.string.wordlist_delete_confirmation, list.wordList.name))
            .setPositiveButton(R.string.wordlist_delete_button) { _, _ ->
                viewModel.deleteList(list.wordList)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun consultList(list: WordListWithWords) {
        val query = "list:\"${list.wordList.name}\""
        val searchView = requireActivity().findViewById<SearchView>(R.id.appbar_search)
        searchView.setQuery(query, true)
    }
}

class WordListAdapter(
    private val context: Context,
    private val onConsultClick: (WordListWithWords) -> Unit,
    private val onRenameClick: (WordListWithWords) -> Unit,
    private val onDeleteClick: (WordListWithWords) -> Unit
) : ListAdapter<WordListWithWords, WordListAdapter.ViewHolder>(WordListDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.fragment_wordlist_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameTextView: TextView = itemView.findViewById(R.id.wordlist_name)
        private val wordCountTextView: TextView = itemView.findViewById(R.id.wordlist_wordcount)
        private val createdDateTextView: TextView = itemView.findViewById(R.id.wordlist_creationdate)
        private val editedDateTextView: TextView = itemView.findViewById(R.id.wordlist_modifdate)
        private val infoCard: View = itemView.findViewById(R.id.wordlist_infos_card)
        private val deleteButton: View = itemView.findViewById(R.id.wordlist_delete_button)
        private val renameButton: View = itemView.findViewById(R.id.wordlist_rename_button)

        fun bind(list: WordListWithWords) {
            nameTextView.text = list.wordList.name
            wordCountTextView.text = itemView.context.getString(
                R.string.wordlist_word_count,
                list.words.size
            )

            val dateFormat = SimpleDateFormat(context.getString(R.string.date_format), Locale.getDefault())

            createdDateTextView.text = context.getString(R.string.wordlist_createddate, dateFormat.format(list.wordList.creationDate))
            editedDateTextView.text = context.getString(R.string.wordlist_editeddate, dateFormat.format(list.wordList.lastModified))

            infoCard.setOnClickListener { onConsultClick(list) }
            deleteButton.setOnClickListener { onDeleteClick(list) }
            renameButton.setOnClickListener { onRenameClick(list) }

            val visibility = if (list.wordList.listType == WordList.ListType.SYSTEM)
                View.INVISIBLE else View.VISIBLE

            deleteButton.visibility = visibility
            renameButton.visibility = visibility
        }
    }
}

class WordListDiffCallback : DiffUtil.ItemCallback<WordListWithWords>() {
    override fun areItemsTheSame(oldItem: WordListWithWords, newItem: WordListWithWords): Boolean {
        return oldItem.wordList.id == newItem.wordList.id
    }

    override fun areContentsTheSame(oldItem: WordListWithWords, newItem: WordListWithWords): Boolean {
        return oldItem == newItem
    }
} 