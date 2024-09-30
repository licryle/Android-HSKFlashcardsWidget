package fr.berliat.hskwidget.ui.dictionary

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import fr.berliat.hskwidget.R
import fr.berliat.hskwidget.data.model.ChineseWord
import fr.berliat.hskwidget.data.store.ChineseWordsStore

class DictionarySearchFragment : Fragment() {

    private lateinit var searchView: SearchView
    private lateinit var recyclerView: RecyclerView
    private lateinit var searchAdapter: DictionarySearchAdapter

    private var isLoading = false
    private var currentPage = 0
    private var itemsPerPage = 50
    private var currentSearch = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_dictionary_search, container, false)

        searchView = view.findViewById(R.id.search_view)
        recyclerView = view.findViewById(R.id.recycler_view)

        setupRecyclerView()
        setupSearchView()

        return view
    }

    private fun setupRecyclerView() {
        searchAdapter = DictionarySearchAdapter()
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = searchAdapter

        // Infinite scroll for pagination
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val totalItemCount = layoutManager.itemCount
                val lastVisibleItem = layoutManager.findLastVisibleItemPosition()

                // Load more if at the bottom and not already loading
                if (!isLoading && totalItemCount <= (lastVisibleItem + 2)) {
                    loadMoreResults()
                }
            }
        })
    }

    private fun setupSearchView() {
        // Set listener to handle search queries
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            // Triggered when the search button is pressed (or search query submitted)
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let {
                    performSearch(it) // Call search method
                }
                return true
            }

            // Triggered when the query text is changed
            override fun onQueryTextChange(newText: String?): Boolean {
                newText?.let {
                    performSearch(it) // Call search method
                }
                return true
            }
        })
    }

    // Search logic: Fetch new data based on the search query
    private fun performSearch(query: String) {
        // Clear current results and reset pagination
        searchAdapter.clearData()
        currentPage = 0
        currentSearch = query
        searchAdapter.addData(fetchResultsForPage(query))
    }

    // Method to load more results with pagination
    private fun loadMoreResults() {
        isLoading = true

        val newResults = fetchResultsForPage(currentSearch)
        searchAdapter.addData(newResults)
        isLoading = false
    }

    // Simulate fetching search results based on the query and current page
    private fun fetchResultsForPage(query: String): List<ChineseWord> {
        val results = ChineseWordsStore.getInstance(requireContext()).findWordFromStrLike(query,
            currentPage, itemsPerPage)

        currentPage++

        return results
    }
}

