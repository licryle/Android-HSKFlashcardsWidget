package fr.berliat.hskwidget.ui.dictionary

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import fr.berliat.hskwidget.R
import fr.berliat.hskwidget.data.model.AnnotatedChineseWord
import fr.berliat.hskwidget.data.store.AppPreferencesStore
import fr.berliat.hskwidget.data.store.ChineseWordsDatabase
import fr.berliat.hskwidget.domain.Utils
import fr.berliat.hskwidget.domain.SearchQueryProcessor
import kotlinx.coroutines.launch

import fr.berliat.hskwidget.databinding.FragmentDictionarySearchBinding
import fr.berliat.hskwidget.databinding.FragmentDictionarySearchItemBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant

class DictionarySearchFragment : Fragment(), DictionarySearchAdapter.SearchResultChangedListener {
    private lateinit var searchAdapter: DictionarySearchAdapter
    private lateinit var binding: FragmentDictionarySearchBinding
    private lateinit var appConfig: AppPreferencesStore
    private val searchQueryProcessor = SearchQueryProcessor()

    private var isLoading = false
    private var currentPage = 0
    private var itemsPerPage = 20
    private val searchQuery: String
        get() {
            return activity?.findViewById<SearchView>(R.id.appbar_search)?.query.toString().trim()
        }

    private var lastFullSearchStartTime = Instant.now().toEpochMilli()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        searchAdapter = DictionarySearchAdapter(requireParentFragment(), this)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentDictionarySearchBinding.inflate(layoutInflater)
        appConfig = AppPreferencesStore(requireContext())

        setupRecyclerView()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        performSearch()
    }

    override fun onResume() {
        super.onResume()

        Utils.logAnalyticsScreenView(requireContext(), "DictionarySearch")
    }

    private fun setupRecyclerView() {
        binding.dictionarySearchResults.layoutManager = LinearLayoutManager(context)
        binding.dictionarySearchResults.adapter = searchAdapter

        binding.dictionarySearchFilterHasannotation.isChecked = appConfig.searchFilterHasAnnotation
        binding.dictionaryShowHsk3definition.isChecked = appConfig.dictionaryShowHSK3Definition

        // Infinite scroll for pagination
        binding.dictionarySearchResults.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                if (dx >= 10 || dy >= 10)
                    Utils.hideKeyboard(requireContext(), requireView())

                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val totalItemCount = layoutManager.itemCount
                val lastVisibleItem = layoutManager.findLastVisibleItemPosition()

                // Load more if at the bottom and not already loading
                if (!isLoading && totalItemCount <= (lastVisibleItem + 2)) {
                    viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) { loadMoreResults() }
                }
            }
        })

        binding.dictionarySearchFilterHasannotation.setOnClickListener {
            appConfig.searchFilterHasAnnotation = binding.dictionarySearchFilterHasannotation.isChecked
            performSearch()
        }

        binding.dictionaryShowHsk3definition.setOnClickListener {
            appConfig.dictionaryShowHSK3Definition = binding.dictionaryShowHsk3definition.isChecked
            searchAdapter.notifyDataSetChanged()
        }
    }

    // Search logic: Fetch new data based on the search query
    fun performSearch() {
        // Clear current results and reset pagination
        isLoading = true
        searchAdapter.clearData()
        currentPage = 0

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            // Here we executed in the coRoutine Scope
            val result: Pair<Long, List<AnnotatedChineseWord>> = Pair(
                Instant.now().toEpochMilli(),
                fetchResultsForPage()
            )

            withContext(Dispatchers.Main) {
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
                binding.dictionarySearchResults.scrollToPosition(0)
            }
        }
    }

    // Method to load more results with pagination
    private suspend fun loadMoreResults() {
        isLoading = true

        Log.d(TAG, "Load more results for currentSearch: $searchQuery")
        val newResults = fetchResultsForPage()
        isLoading = false

        withContext(Dispatchers.Main) {
            searchAdapter.addData(newResults)
        }
    }

    // Simulate fetching search results based on the query and current page
    private suspend fun fetchResultsForPage(): List<AnnotatedChineseWord> {
        Log.d(TAG, "Searching for $searchQuery")
        val dao = ChineseWordsDatabase.getInstance(requireContext()).annotatedChineseWordDAO()
        try {
            val annotatedOnly = binding.dictionarySearchFilterHasannotation.isChecked
            val (listName, otherFilters) = searchQueryProcessor.processSearchQuery(searchQuery)
            
            val results = if (listName != null) {
                // Search within the specified word list
                dao.searchFromWordList(listName, annotatedOnly, currentPage, itemsPerPage)
            } else {
                dao.searchFromStrLike(otherFilters ?: searchQuery, annotatedOnly, currentPage, itemsPerPage)
            }
            Log.d(TAG, "Search returned for $searchQuery")

            currentPage++

            return results
        } catch (e: Exception) {
            // Code for handling the exception
            Log.e(TAG, "$e")
        }

        return emptyList()
    }

    private fun evaluateEmptyView() {
        if (isLoading) {
            binding.dictionarySearchLoading.visibility = View.VISIBLE
            binding.dictionarySearchNoresults.visibility = View.GONE
            return
        }

        // Not loading
        binding.dictionarySearchLoading.visibility = View.GONE

        // Does the search have an exact match in dictionary?
        if (searchQuery.isEmpty()
            || ! searchAdapter.getData().none { it.simplified == searchQuery }
            || ! Utils.containsChinese(searchQuery)) {
            binding.dictionarySearchNoresults.visibility = View.GONE
        } else {
            val text = binding.dictionaryNoresultText
            text.text = getString(R.string.dictionary_noresult_text).format(searchQuery)

            binding.dictionarySearchNoresults.setOnClickListener {
                val action =
                    DictionarySearchFragmentDirections.annotateWord(searchQuery, true)

                findNavController().navigate(action)
            }

            binding.dictionarySearchNoresults.visibility = View.VISIBLE
        }
    }

    override fun onDataChanged(newData: List<AnnotatedChineseWord>) {
        evaluateEmptyView()
    }

    class SearchResultItem(private val binding: FragmentDictionarySearchItemBinding,
                           private val navController: NavController) : RecyclerView.ViewHolder(binding.root) {

        fun bind(result: AnnotatedChineseWord) {
            Utils.populateDictionaryEntryView(binding, result, navController)
        }
    }

    companion object {
        const val TAG = "DictionarySearchFragment"
    }
}

