package fr.berliat.hsktextviews.views

import android.content.Context
import android.graphics.Color
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

class HSKWordsAdapter(private val context: Context,
                      private val listener: HSKWordClickListener)
    : RecyclerView.Adapter<HSKTextView.HSKWordsHolder>() {

    private val words = mutableListOf<Pair<String, String>>()

    var clickedWords: MutableMap<String, String> = mutableMapOf()
        set(words) {
            field = words
            notifyDataSetChanged()
        }

    var hanziSize: Int = DEFAULT_HANZI_SIZE
        set(size) {
            field = size
            notifyDataSetChanged()
        }

    var wordSeparator: String = DEFAULT_WORD_SEPARATOR
        set(separator) {
            field = separator
            notifyDataSetChanged()
        }

    var clickedBackgroundColor: Int = DEFAULT_CLICKED_BG_COLOR
        set(color) {
            field = color
            notifyDataSetChanged()
        }

    var clickedHanziColor: Int = DEFAULT_CLICKED_TXT_COLOR
        set(color) {
            field = color
            notifyDataSetChanged()
        }

    var clickedPinyinColor: Int = DEFAULT_CLICKED_TXT_COLOR
        set(color) {
            field = color
            notifyDataSetChanged()
        }

    val wordsFrequency: Map<String, Int>
        get() {
            val freq = mutableMapOf<String, Int>()
            for (word in words) {
                freq[word.first] = freq.getOrDefault(word.first, 0) + 1
            }
            return freq.toMap() // Return the immutable map
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
        var word = words[position]
        if (word.first in clickedWords)
            word = word.copy(second = clickedWords[word.first].toString())

        holder.bind(word, hanziSize, wordSeparator,
                    clickedBackgroundColor, clickedHanziColor, clickedPinyinColor,
            word.first in clickedWords)
    }

    companion object {
        private const val DEFAULT_HANZI_SIZE = 20
        private const val DEFAULT_WORD_SEPARATOR = "/"
        private const val DEFAULT_CLICKED_BG_COLOR = Color.BLACK
        private const val DEFAULT_CLICKED_TXT_COLOR = Color.WHITE
    }
}