package fr.berliat.hskwidget.ui.dictionary

import android.content.Context
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import fr.berliat.hsktextviews.views.HSKWordView

import fr.berliat.hskwidget.R
import fr.berliat.hskwidget.data.dao.AnnotatedChineseWord
import fr.berliat.hskwidget.data.model.ChineseWord
import java.util.Locale

class DictionarySearchAdapter(private val context: Context,
                              private val fragment: Fragment
)  :
    RecyclerView.Adapter<DictionarySearchAdapter.SearchResultItem>() {

    private val results = mutableListOf<AnnotatedChineseWord>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchResultItem {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.fragment_dictionary_search_item, parent, false)
        return SearchResultItem(context, fragment, view)
    }

    override fun onBindViewHolder(holder: SearchResultItem, position: Int) {
        holder.bind(results[position])
    }

    override fun getItemCount(): Int {
        return results.size
    }

    // Method to add data to the adapter
    fun addData(newResults: List<AnnotatedChineseWord>) {
        results.addAll(newResults)
        notifyDataSetChanged()  // You can optimize with notifyItemRangeInserted
    }

    // Method to clear existing data
    fun clearData() {
        results.clear()
        notifyDataSetChanged()
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
                val action = DictionarySearchFragmentDirections.annotateWord(result.simplified!!)

                fragment.findNavController().navigate(action)
            }
        }
    }
}
