package fr.berliat.hskwidget.ui.dictionary

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView

import fr.berliat.hskwidget.data.dao.AnnotatedChineseWord
import fr.berliat.hskwidget.databinding.FragmentDictionarySearchItemBinding


class DictionarySearchAdapter(private val fragment: Fragment,
                              private val dataChangedListener: SearchResultChangedListener
)  :
    RecyclerView.Adapter<DictionarySearchFragment.SearchResultItem>() {

    private val results = mutableListOf<AnnotatedChineseWord>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DictionarySearchFragment.SearchResultItem {
        val binding = FragmentDictionarySearchItemBinding.inflate(
            LayoutInflater.from(parent.context), parent, false)

        return DictionarySearchFragment.SearchResultItem(binding, fragment.findNavController())
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

    private fun notifySearchResultsChange() {
        notifyDataSetChanged() // You can optimize with notifyItemRangeInserted
        dataChangedListener.onDataChanged(results)  // Notify the activity or fragment
    }

    interface SearchResultChangedListener {
        fun onDataChanged(newData: List<AnnotatedChineseWord>)
    }
}
