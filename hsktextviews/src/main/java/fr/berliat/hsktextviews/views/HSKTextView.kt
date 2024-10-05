package fr.berliat.hsktextviews.views

import android.content.Context
import android.util.AttributeSet
import androidx.recyclerview.widget.RecyclerView
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.JustifyContent
import com.google.android.flexbox.FlexWrap

import fr.berliat.hsktextviews.R

class HSKTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : RecyclerView(context, attrs, defStyleAttr), OnHSKWordClickListener {
    private lateinit var clickListener: (HSKWordView) -> Unit?
    private val wordsAdapter: HSKWordsAdapter
    private var originalText : String = ""
    private val loManager: FlexboxLayoutManager = FlexboxLayoutManager(context)

    init {
        loManager.flexDirection = FlexDirection.ROW
        loManager.flexWrap = FlexWrap.WRAP
        loManager.justifyContent = JustifyContent.FLEX_START
        layoutManager = loManager
        wordsAdapter = HSKWordsAdapter(this)
        adapter = wordsAdapter


        attrs?.let {
            val typedArray = context.obtainStyledAttributes(it, R.styleable.HSKTextView, 0, 0)
            //text = typedArray.getString(R.styleable.HSKTextView_text).toString()
            typedArray.recycle()
        }
    }

    var text: String
        get() = originalText
        set(value) {
            originalText = value
            //val result = ToAnalysis.parse(value)

            println(value)
            //println(result)

            val words = mutableListOf<Pair<String, String>>()
            //result.terms.forEach {

            // @todo(Licryle): find a way to split words
            value.chunked(2).forEach {
                words.add(Pair(it, hanziToPinyin(it)))
            }

            wordsAdapter.addData(words)
        }

    fun hanziToPinyin(hanzi: String): String {
        // @todo(Licryle): look up pinyin
        return "hanzi"
    }

    fun setOnWordClickListener(listener: (HSKWordView) -> Unit) {
        clickListener = listener
    }

    override fun onWordClick(wordView: HSKWordView) {
        if (clickListener != null)
            clickListener(wordView)
    }

    class HSKWordsHolder(private val itemView: HSKWordView,
                         private val listener: OnHSKWordClickListener)
        : ViewHolder(itemView) {

        fun bind(word: Pair<String, String>) {
            val view = (itemView as HSKWordView)
            view.hanziText = word.first
            view.pinyinText = word.second
            //pinyinSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 20f, resources.displayMetrics)

            view.setOnWordClickListener(listener)
        }
    }
}