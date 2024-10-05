package fr.berliat.hskwidget.ui.dictionary

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView

import fr.berliat.hskwidget.R
import fr.berliat.hskwidget.data.dao.AnnotatedChineseWord


class DictionarySearchAdapter(private val context: Context,
                              private val fragment: Fragment
)  :
    RecyclerView.Adapter<DictionarySearchFragment.SearchResultItem>() {

    private val results = mutableListOf<AnnotatedChineseWord>()
    private lateinit var dataChangedListener : SearchResultChangedListener

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DictionarySearchFragment.SearchResultItem {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.fragment_dictionary_search_item, parent, false)
        return DictionarySearchFragment.SearchResultItem(context, fragment, view)
    }

    override fun onBindViewHolder(holder: DictionarySearchFragment.SearchResultItem, position: Int) {
        holder.bind(results[position])
    }

    override fun getItemCount(): Int {
        return results.size
    }

    // Method to add data to the adapter
    fun addData(newResults: List<AnnotatedChineseWord>) {
        results.addAll(newResults)
        notifySearchResultsChange()
    }

    // Method to clear existing data
    fun clearData() {
        results.clear()
        notifySearchResultsChange()
    }

    fun setSearchResultsChangeListener(listener: SearchResultChangedListener) {
        dataChangedListener = listener
    }

    private fun notifySearchResultsChange() {
        notifyDataSetChanged() // You can optimize with notifyItemRangeInserted
        dataChangedListener.onDataChanged(results)  // Notify the activity or fragment
    }

    interface SearchResultChangedListener {
        fun onDataChanged(newData: List<AnnotatedChineseWord>)
    }
}
