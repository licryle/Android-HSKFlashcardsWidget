package fr.berliat.hskwidget.ui.assistant

import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import fr.berliat.hskwidget.data.model.WriteAssist
import fr.berliat.hskwidget.data.store.AppPreferencesStore
import fr.berliat.hskwidget.domain.Utils
import kotlinx.coroutines.launch

import fr.berliat.hskwidget.databinding.FragmentWriteAssistBinding
import fr.berliat.hskwidget.databinding.FragmentWriteAssistItemBinding
import fr.berliat.hskwidget.domain.GenAI
import fr.berliat.hskwidget.ui.BindableViewHolder
import fr.berliat.hskwidget.ui.GenericRecyclerAdapter

class WriteAssistView(private val binding: FragmentWriteAssistItemBinding) : BindableViewHolder<WriteAssist>(binding.root) {
    override fun bind(result: WriteAssist) {
        with (binding) {
            writeassistConfidence.text = doubleToPercent(result.confidence)
            writeassistGrade.text = doubleToPercent(result.grade)

            writeassistFixedCn.text = result.correctedCN
            writeassistFixedEn.text = result.correctedEN
            writeassistOriginalCn.text = result.originalCN
            writeassistOriginalEn.text = result.originalEN

            writeassistExplanations.text = result.explanations
        }
    }

    private fun doubleToPercent(d: Double): String {
        var p = d.coerceAtLeast(0.0)
        p = p.coerceAtMost(1.0)
        return String.format("%.0f%%", p * 100)
    }
}

class WriteAssistFragment : Fragment(), GenericRecyclerAdapter.ItemChangedListener<WriteAssist> {
    private lateinit var searchAdapter: GenericRecyclerAdapter<WriteAssist, WriteAssistView, FragmentWriteAssistItemBinding>
    private lateinit var binding: FragmentWriteAssistBinding

    private var isLoading = false
    private var currentPage = 0
    private var itemsPerPage = 20

    private lateinit var genAI: GenAI

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        searchAdapter = GenericRecyclerAdapter(
            FragmentWriteAssistItemBinding::inflate,
            this
        ) { WriteAssistView(it) }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentWriteAssistBinding.inflate(layoutInflater)
        genAI = GenAI(AppPreferencesStore(requireContext()).groqAPIKey)

        setupRecyclerView()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //performSearch()
        //@Todo: add reader for passed param when we add "open application to fix sentence"
    }

    override fun onResume() {
        super.onResume()

        Utils.logAnalyticsScreenView(requireContext(), "DictionarySearch")
    }

    private fun setupRecyclerView() {
        binding.writeassistAssists.layoutManager = LinearLayoutManager(context)
        binding.writeassistAssists.adapter = searchAdapter

        // Infinite scroll for pagination
        binding.writeassistAssists.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                if (dx >= 10 || dy >= 10)
                    Utils.hideKeyboard(requireContext(), requireView())

                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val totalItemCount = layoutManager.itemCount
                val lastVisibleItem = layoutManager.findLastVisibleItemPosition()

                // Load more if at the bottom and not already loading
                if (!isLoading && totalItemCount <= (lastVisibleItem + 2)) {
                    //viewLifecycleOwner.lifecycleScope.launch { loadMoreResults() }
                }
            }
        })

        binding.writeassistSendboxSend.setOnClickListener {
            performWriteAssist(binding.writeassistSendboxText.text.toString())
        }
        binding.writeassistSendboxText.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN) {
                binding.writeassistSendboxSend.callOnClick()
                true
            } else {
                false
            }
        }
    }

    private fun performWriteAssist(text: String) {
        isLoading = true
        viewLifecycleOwner.lifecycleScope.launch {
            // Here we executed in the coRoutine Scope
            try {
                val assist = genAI.fixSentence(text)

                // Update the UI with the result
                isLoading = false
                searchAdapter.addData(mutableListOf(assist))
            } catch(e: Exception) {
                Log.d(TAG, "Error getting a WriteAssist: $e")
                Toast.makeText(requireContext(), "Error getting a WriteAssist: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Search logic: Fetch new data based on the search query
    /*fun performSearch() {
        // Clear current results and reset pagination
        isLoading = true
        searchAdapter.clearData()
        currentPage = 0

        viewLifecycleOwner.lifecycleScope.launch {
            // Here we executed in the coRoutine Scope
            val result: Pair<Long, List<AnnotatedChineseWord>> = Pair(
                System.currentTimeMillis(),
                fetchResultsForPage()
            )

            // Switch back to the main thread to update UI
            // Protecting against concurrent searches (typing fast etc)
            if (result.first >= lastFullSearchStartTime) {
                lastFullSearchStartTime = result.first
                searchAdapter.clearData()
                currentPage = 1
            }

            // Update the UI with the result
            isLoading = false
            searchAdapter.addData(result.second)
            binding.dictionarySearchResults.scrollToPosition(0) // @TODO(Licryle): chase down the bug that keeps the screen blank, sometimes.
        }
    }

    // Method to load more results with pagination
    private suspend fun loadMoreResults() {
        isLoading = true

        Log.d(TAG, "Load more assists")
        val newResults = fetchItemsForPage()
        isLoading = false
        searchAdapter.addData(newResults)
    }

    // Simulate fetching search results based on the query and current page
    private suspend fun fetchItemsForPage(): List<AnnotatedChineseWord> {
        Log.d(TAG, "fetchItemsForPage $currentPage")
        val db = ChineseWordsDatabase.getInstance(requireContext())
        val dao = db.annotatedChineseWordDAO()
        try {
            val results = dao.searchFromStrLike(searchQuery, annotatedOnly, currentPage, itemsPerPage)
            Log.d(TAG, "Search returned for $searchQuery")

            currentPage++

            return results
        } catch (e: Exception) {
            // Code for handling the exception
            Log.e("DictionarySearchFragment", "$e")
        }

        return emptyList()
    }*/

    private fun evaluateEmptyView() {
        // Show/hide empty view based on data
        /*if (searchAdapter.itemCount == 0) {
            if (isLoading) {
                binding.dictionarySearchLoading.visibility = View.VISIBLE
                binding.dictionarySearchNoresults.visibility = View.GONE
                binding.dictionarySearchResults.visibility = View.GONE
            } else {
                val text = binding.dictionaryNoresultText
                text.text = getString(R.string.dictionary_noresult_text).format(searchQuery)

                binding.dictionarySearchNoresults.setOnClickListener {
                    val action =
                        DictionarySearchFragmentDirections.annotateWord(searchQuery, true)

                    findNavController().navigate(action)
                }

                binding.dictionarySearchLoading.visibility = View.GONE
                binding.dictionarySearchNoresults.visibility = View.VISIBLE
                binding.dictionarySearchResults.visibility = View.GONE
            }
        } else {
            binding.dictionarySearchLoading.visibility = View.GONE
            binding.dictionarySearchNoresults.visibility = View.GONE
            binding.dictionarySearchResults.visibility = View.VISIBLE
        }*/
    }

    override fun onDataChanged(newData: List<WriteAssist>) {
        evaluateEmptyView()
    }

    companion object {
        private const val TAG = "WriteAssistFragment"
    }
}

