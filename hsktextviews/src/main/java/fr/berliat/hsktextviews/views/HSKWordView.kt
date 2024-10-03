package fr.berliat.hsktextviews.views

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.util.AttributeSet
import android.widget.LinearLayout
import android.widget.TextView
import fr.berliat.hsktextviews.R

interface OnHSKWordClickListener {
    fun onWordClick(wordView: HSKWordView)
}

class HSKWordView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {
    private val pinyinView: TextView
    private val hanziView: TextView
    private var clickListener: OnHSKWordClickListener? = null

    init {
        inflate(context, R.layout.hsk_word_view, this)
        pinyinView = findViewById(R.id.pinyin)
        hanziView = findViewById(R.id.hanzi)

        pinyinView.setOnClickListener { clickListener?.onWordClick(this) }
        hanziView.setOnClickListener { clickListener?.onWordClick(this) }

        attrs?.let {
            val typedArray = context.obtainStyledAttributes(it, R.styleable.HSKWordView, 0, 0)
            pinyinText = typedArray.getString(R.styleable.HSKWordView_pinyin).toString()
            hanziText = typedArray.getString(R.styleable.HSKWordView_hanzi).toString()
            pinyinText = typedArray.getString(R.styleable.HSKWordView_pinyin) ?: ""
            hanziText = typedArray.getString(R.styleable.HSKWordView_hanzi) ?: ""

            pinyinSize = typedArray.getDimensionPixelSize(R.styleable.HSKWordView_pinyinSize, 27)
            hanziSize = typedArray.getDimensionPixelSize(R.styleable.HSKWordView_hanziSize, 36)

            pinyinColor = typedArray.getColor(R.styleable.HSKWordView_pinyinColor, Color.DKGRAY)
            hanziColor = typedArray.getColor(R.styleable.HSKWordView_hanziColor, Color.BLACK)

            pinyinStyle = typedArray.getInt(R.styleable.HSKWordView_pinyinStyle, Typeface.NORMAL)
            hanziStyle = typedArray.getInt(R.styleable.HSKWordView_hanziStyle, Typeface.BOLD)

            typedArray.recycle()
        }
    }

    fun setOnClickListener(listener: OnHSKWordClickListener) {
        clickListener = listener
    }

    var pinyinText: String
        get() = pinyinView.text.toString()
        set(value) {
            pinyinView.text = value
        }

    var hanziText: String
        get() = hanziView.text.toString()
        set(value) {
            hanziView.text = value
        }

    var pinyinSize: Int
        get() = pinyinView.textSize.toInt()
        set(value) {
            pinyinView.textSize = value.toFloat()
        }

    var hanziSize: Int
        get() = hanziView.textSize.toInt()
        set(value) {
            hanziView.textSize = value.toFloat()
        }

    var pinyinColor: Int
        get() = pinyinView.currentTextColor
        set(value) {
            pinyinView.setTextColor(value)
        }

    var hanziColor: Int
        get() = hanziView.currentTextColor
        set(value) {
            hanziView.setTextColor(value)
        }

    var pinyinStyle: Int
        get() = pinyinView.typeface.style
        set(value) {
            pinyinView.setTypeface(null, value)
        }

    var hanziStyle: Int
        get() = hanziView.typeface.style
        set(value) {
            hanziView.setTypeface(null, value)
        }
}
