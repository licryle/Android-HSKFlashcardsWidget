package fr.berliat.hskwidget.ui.dictionary

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

import fr.berliat.hskwidget.R
import fr.berliat.hskwidget.data.model.ChineseWord
import java.util.Locale

class DictionarySearchAdapter : RecyclerView.Adapter<DictionarySearchAdapter.SearchViewHolder>() {

    private val results = mutableListOf<ChineseWord>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.fragment_dictionary_search_item, parent, false)
        return SearchViewHolder(view)
    }

    override fun onBindViewHolder(holder: SearchViewHolder, position: Int) {
        holder.bind(results[position])
    }

    override fun getItemCount(): Int {
        return results.size
    }

    // Method to add data to the adapter
    fun addData(newResults: List<ChineseWord>) {
        results.addAll(newResults)
        notifyDataSetChanged()  // You can optimize with notifyItemRangeInserted
    }

    // Method to clear existing data
    fun clearData() {
        results.clear()
        notifyDataSetChanged()
    }

    class SearchViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val hanziView: TextView = itemView.findViewById(R.id.hanzi)
        private val hskView: TextView = itemView.findViewById(R.id.hsk_level)
        private val pinyinView: TextView = itemView.findViewById(R.id.pinyin)
        private val definitionView: TextView = itemView.findViewById(R.id.definition)

        fun bind(result: ChineseWord) {
            hanziView.text = result.simplified
            hskView.text = result.HSK.toString()
            pinyinView.text = result.pinyins.toString()
            definitionView.text = result.definition[Locale.ENGLISH]
        }
    }
}
