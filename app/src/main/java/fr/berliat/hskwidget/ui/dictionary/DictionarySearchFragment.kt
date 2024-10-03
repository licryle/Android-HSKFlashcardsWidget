package fr.berliat.hskwidget.ui.dictionary

import android.content.Context
import kotlinx.coroutines.async
import kotlinx.coroutines.GlobalScope

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import fr.berliat.hskwidget.R
import fr.berliat.hskwidget.data.dao.AnnotatedChineseWord
import fr.berliat.hskwidget.data.store.ChineseWordsDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DictionarySearchFragment : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var searchAdapter: DictionarySearchAdapter

    private var isLoading = false
    private var currentPage = 0
    private var itemsPerPage = 20
    private var currentSearch = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_dictionary_search, container, false)

        recyclerView = view.findViewById(R.id.recycler_view)

        setupRecyclerView()

        return view
    }

    private fun setupRecyclerView() {
        searchAdapter = DictionarySearchAdapter(requireContext(), requireParentFragment())
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = searchAdapter

        // Infinite scroll for pagination
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                if (dx >= 10 || dy >= 10)
                    hideKeyboard()

                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val totalItemCount = layoutManager.itemCount
                val lastVisibleItem = layoutManager.findLastVisibleItemPosition()

                // Load more if at the bottom and not already loading
                if (!isLoading && totalItemCount <= (lastVisibleItem + 2)) {
                    GlobalScope.async { loadMoreResults() }
                }
            }
        })
    }

    // Search logic: Fetch new data based on the search query
    fun performSearch(query: String) {
        // Clear current results and reset pagination
        searchAdapter.clearData()
        currentPage = 0
        currentSearch = query

        GlobalScope.launch {
            // Switch to the IO dispatcher to perform background work
            val result = withContext(Dispatchers.IO) {
                fetchResultsForPage(query)
            }

            // Switch back to the main thread to update UI
            withContext(Dispatchers.Main) {
                // Update the UI with the result
                searchAdapter.addData(result)
                isLoading = false
                recyclerView.scrollToPosition(0) // @TODO(Licryle): chase down the bug that keeps the screen blank, sometimes.
            }
        }
    }

    // Method to load more results with pagination
    private suspend fun loadMoreResults() {
        isLoading = true

        val newResults = fetchResultsForPage(currentSearch)
        searchAdapter.addData(newResults)
        isLoading = false
    }

    // Simulate fetching search results based on the query and current page
    private suspend fun fetchResultsForPage(query: String): List<AnnotatedChineseWord> {
        Log.d("DictionarySearchFragment", "Searching for $query")
        val db = ChineseWordsDatabase.getInstance(requireContext())
        val dao = db.annotatedChineseWordDAO()
        try {
            val results = dao.findWordFromStrLike(query, currentPage, itemsPerPage)
            Log.d("DictionarySearchFragment", "Search returned for $query")

            currentPage++

            return results
        } catch (e: Exception) {
            // Code for handling the exception
            Log.e("DictionarySearchFragment", "$e")
        }

        return emptyList()
    }

    fun hideKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(requireView().windowToken , 0)
    }
}

