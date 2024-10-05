package fr.berliat.hsktextviews.views

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

class HSKWordsAdapter(private val listener: OnHSKWordClickListener) : RecyclerView.Adapter<HSKTextView.HSKWordsHolder>() {
    private val words = mutableListOf<Pair<String, String>>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HSKTextView.HSKWordsHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(fr.berliat.hsktextviews.R.layout.hsk_word_holder, parent, false)
        return HSKTextView.HSKWordsHolder((view as HSKWordView), listener)
    }

    override fun getItemCount(): Int {
        return words.size
    }

    // Method to add data to the adapter
    fun addData(newResults: List<Pair<String, String>>) {
        words.addAll(newResults)
        notifyDataSetChanged()  // You can optimize with notifyItemRangeInserted
    }

    // Method to clear existing data
    fun clearData() {
        words.clear()
        notifyDataSetChanged()
    }

    override fun onBindViewHolder(holder: HSKTextView.HSKWordsHolder, position: Int) {
        holder.bind(words[position])
    }
}