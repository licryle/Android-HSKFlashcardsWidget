package fr.berliat.hskwidget.ui.wordlist

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import fr.berliat.hskwidget.R
import fr.berliat.hskwidget.data.model.WordList
import fr.berliat.hskwidget.data.model.WordListWithCount
import fr.berliat.hskwidget.ui.utils.AnkiDelegate
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

        viewModel = WordListViewModel(ankiDelegate.wordListRepo)

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

        refreshLists()
    }

    private fun refreshLists() {
        viewModel.getAllLists { wordLists, _ -> adapter.submitList(wordLists) }
    }

    private fun showRenameDialog(list: WordListWithCount) {
        val dialog = WordListReNameDialog.newInstance(listId = list.id, listName = list.name) {
            _, _ -> refreshLists()
        }

        dialog.show(parentFragmentManager, "RenameListDialog")
    }

    private fun showCreateListDialog() {
        val dialog = WordListReNameDialog { _, _ ->
            refreshLists()
        }

        dialog.show(childFragmentManager, "CreateListDialog")
    }

    private fun showDeleteConfirmation(list: WordListWithCount) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.wordlist_delete_button)
            .setMessage(getString(R.string.wordlist_delete_confirmation, list.name))
            .setPositiveButton(R.string.wordlist_delete_button) { _, _ ->
                viewModel.deleteList(list.wordList, callback = { _ ->
                    refreshLists()
                })
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun consultList(list: WordListWithCount) {
        val query = "list:\"${list.name}\""
        val searchView = requireActivity().findViewById<SearchView>(R.id.appbar_search)
        searchView.setQuery(query, true)
    }
}

class WordListAdapter(
    private val context: Context,
    private val onConsultClick: (WordListWithCount) -> Unit,
    private val onRenameClick: (WordListWithCount) -> Unit,
    private val onDeleteClick: (WordListWithCount) -> Unit
) : ListAdapter<WordListWithCount, WordListAdapter.ViewHolder>(WordListDiffCallback()) {

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

        fun bind(list: WordListWithCount) {
            nameTextView.text = list.wordList.name
            wordCountTextView.text = itemView.context.getString(
                R.string.wordlist_word_count,
                list.wordCount
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

class WordListDiffCallback : DiffUtil.ItemCallback<WordListWithCount>() {
    override fun areItemsTheSame(oldItem: WordListWithCount, newItem: WordListWithCount): Boolean {
        return oldItem.wordList.id == newItem.wordList.id
    }

    override fun areContentsTheSame(oldItem: WordListWithCount, newItem: WordListWithCount): Boolean {
        return oldItem == newItem
    }
} 