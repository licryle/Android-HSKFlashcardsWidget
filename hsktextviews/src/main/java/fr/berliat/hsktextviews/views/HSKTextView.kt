package fr.berliat.hsktextviews.views

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import fr.berliat.hsktextviews.R

interface OnHSKTextClickListener {
    fun onWordClick(wordView: HSKWordView)
}

// @todo(Licryle): finish implementing, so abstract for now
abstract class HSKTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr), OnHSKWordClickListener {
    private var clickListener: OnHSKTextClickListener? = null

    init {
        inflate(context, R.layout.hsk_text_view, this)

        attrs?.let {
            val typedArray = context.obtainStyledAttributes(it, R.styleable.HSKTextView, 0, 0)
            val hskText = typedArray.getString(R.styleable.HSKTextView_text)
            typedArray.recycle()

            if (hskText != null)
                setText(hskText)
        }
    }

    fun addWord(hanzi: String, pinyin: String) {
        val wordView = this

        val itemView = HSKWordView(context).apply {
            hanziText = hanzi
            pinyinText = pinyin
            setOnClickListener(wordView)
        }
        addView(itemView)
    }

    fun setText(text: String) {
        // @todo(Licryle): break text properly
        text.chunked(1)?.forEach {
            val hanzi = it
            val pinyin = hanziToPinyin(hanzi)
            addWord(hanzi, pinyin)
        }
    }

    fun hanziToPinyin(hanzi: String): String {
        // @todo(Licryle): look up pinyin
        return hanzi
    }

    fun setOnClickListener(listener: OnHSKTextClickListener) {
        clickListener = listener
    }

    override fun onWordClick(wordView: HSKWordView) {
        clickListener?.onWordClick(wordView)
    }
}