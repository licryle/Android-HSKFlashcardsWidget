package fr.berliat.hsktextviews.views

import android.content.Context
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

class HSKWordsAdapter(private val context: Context,
                      private val listener: OnHSKWordClickListener,
                      hanziTextSize: Int,
                      wordEndSeparator: String
) : RecyclerView.Adapter<HSKTextView.HSKWordsHolder>() {
    private val words = mutableListOf<Pair<String, String>>()

    var hanziSize: Int = hanziTextSize
        set(size) {
            field = size
            notifyDataSetChanged()
        }

    var wordSeparator: String = wordEndSeparator
        set(separator) {
            field = separator
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HSKTextView.HSKWordsHolder {
        val hskWord = HSKWordView(context)

        return HSKTextView.HSKWordsHolder(hskWord, listener)
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
        holder.bind(words[position], hanziSize, wordSeparator)
    }
}