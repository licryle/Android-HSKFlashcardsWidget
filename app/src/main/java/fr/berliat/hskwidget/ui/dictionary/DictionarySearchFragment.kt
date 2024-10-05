package fr.berliat.hskwidget.ui.dictionary

import android.content.Context
import android.content.res.ColorStateList

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.widget.SearchView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import fr.berliat.hsktextviews.views.HSKWordView

import fr.berliat.hskwidget.R
import fr.berliat.hskwidget.data.dao.AnnotatedChineseWord
import fr.berliat.hskwidget.data.model.ChineseWord
import fr.berliat.hskwidget.data.store.AppPreferencesStore
import fr.berliat.hskwidget.data.store.ChineseWordsDatabase
import fr.berliat.hskwidget.domain.Utils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale

import fr.berliat.hskwidget.databinding.FragmentDictionarySearchBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlin.coroutines.CoroutineContext

class DictionarySearchFragment : Fragment(), DictionarySearchAdapter.SearchResultChangedListener {
    private lateinit var searchAdapter: DictionarySearchAdapter
    private lateinit var binding: FragmentDictionarySearchBinding
    private lateinit var appConfig: AppPreferencesStore

    private var isLoading = false
    private var currentPage = 0
    private var itemsPerPage = 20
    private val searchQuery: String
        get() {
            return activity?.findViewById<SearchView>(R.id.appbar_search)?.query.toString()
        }

    private val coContext: CoroutineContext = Dispatchers.Main
    private var coScope = CoroutineScope(coContext + SupervisorJob())


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentDictionarySearchBinding.inflate(layoutInflater)
        appConfig = AppPreferencesStore(requireContext())

        setupRecyclerView()

        performSearch()

        return binding.root
    }

    override fun onStop() {
        super.onStop()
        coScope.cancel()
    }

    private fun setupRecyclerView() {
        searchAdapter = DictionarySearchAdapter(requireContext(), requireParentFragment())
        binding.dictionarySearchResults.layoutManager = LinearLayoutManager(context)
        searchAdapter.setSearchResultsChangeListener(this)
        binding.dictionarySearchResults.adapter = searchAdapter

        binding.dictionarySearchFilterHasannotation.isChecked = appConfig.searchFilterHasAnnotation

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
                    coScope.launch { loadMoreResults() }
                }
            }
        })

        binding.dictionarySearchFilterHasannotation.setOnClickListener {
            appConfig.searchFilterHasAnnotation = binding.dictionarySearchFilterHasannotation.isChecked
            print(appConfig.searchFilterHasAnnotation)
            performSearch()
        }
    }

    // Search logic: Fetch new data based on the search query
    fun performSearch() {
        // Clear current results and reset pagination
        isLoading = true
        searchAdapter.clearData()
        currentPage = 0
        Log.d("DictionarySearchFragment", "New search requested: $searchQuery")

        coScope.cancel() // avoid a slower search to return and override result!
        coScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

        coScope.launch {
            // Here we executed in the coRoutine Scope
            val result = fetchResultsForPage()

            // Switch back to the main thread to update UI
            // Update the UI with the result
            isLoading = false
            searchAdapter.addData(result)
            binding.dictionarySearchResults.scrollToPosition(0) // @TODO(Licryle): chase down the bug that keeps the screen blank, sometimes.
        }
    }

    // Method to load more results with pagination
    private suspend fun loadMoreResults() {
        isLoading = true

        Log.d("DictionarySearchFragment", "Load more results for currentSearch: $searchQuery")
        val newResults = fetchResultsForPage()
        searchAdapter.addData(newResults)
    }

    // Simulate fetching search results based on the query and current page
    private suspend fun fetchResultsForPage(): List<AnnotatedChineseWord> {
        Log.d("DictionarySearchFragment", "Searching for $searchQuery")
        val db = ChineseWordsDatabase.getInstance(requireContext())
        val dao = db.annotatedChineseWordDAO()
        try {
            val annotatedOnly = binding.dictionarySearchFilterHasannotation.isChecked
            val results = dao.searchFromStrLike(searchQuery, annotatedOnly, currentPage, itemsPerPage)
            Log.d("DictionarySearchFragment", "Search returned for $searchQuery")

            currentPage++

            return results
        } catch (e: Exception) {
            // Code for handling the exception
            Log.e("DictionarySearchFragment", "$e")
        }

        return emptyList()
    }

    private fun evaluateEmptyView() {
        // Show/hide empty view based on data
        if (searchAdapter.itemCount == 0) {
            if (isLoading) {
                binding.dictionarySearchLoading.visibility = View.VISIBLE
                binding.dictionarySearchNoresults.visibility = View.GONE
                binding.dictionarySearchResults.visibility = View.GONE
            } else {
                val text = binding.dictionarySearchNoresults.findViewById<TextView>(R.id.dictionary_noresult_text)
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
        }
    }

    override fun onDataChanged(newData: List<AnnotatedChineseWord>) {
        evaluateEmptyView()
    }

    class SearchResultItem(private val context: Context,
                           private val fragment: Fragment,
                           itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val hanziView: HSKWordView = itemView.findViewById(R.id.dictionary_item_chinese)
        private val hskView: TextView = itemView.findViewById(R.id.dictionary_item_hsk_level)
        private val definitionView: TextView = itemView.findViewById(R.id.dictionary_item_definition)
        private val favView: ImageView = itemView.findViewById(R.id.dictionary_item_favorite)

        fun bind(result: AnnotatedChineseWord) {
            hanziView.hanziText = result.simplified.toString()

            var hskViz = View.VISIBLE
            if (result.word?.hskLevel == null || result.word.hskLevel == ChineseWord.HSK_Level.NOT_HSK)
                hskViz = View.INVISIBLE
            hskView.visibility = hskViz
            hskView.text = result.word?.hskLevel.toString()
            hanziView.pinyinText = result.word?.pinyins.toString()
            definitionView.text = result.word?.definition?.get(Locale.ENGLISH) ?: ""

            if (result.hasAnnotation()) {
                favView.setImageResource(R.drawable.bookmark_heart_24px)
                favView.imageTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(context, R.color.md_theme_dark_inversePrimary))
            } else {
                favView.setImageResource(R.drawable.bookmark_24px)
                favView.imageTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(context, R.color.md_theme_dark_surface))
            }

            favView.setOnClickListener {
                val action = DictionarySearchFragmentDirections.annotateWord(result.simplified!!, false)

                fragment.findNavController().navigate(action)
            }
        }
    }
}

