package fr.berliat.hsktextviews.views

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout

import fr.berliat.hsktextviews.R
import fr.berliat.hsktextviews.databinding.HskWordViewBinding

interface OnHSKWordClickListener {
    fun onWordClick(wordView: HSKWordView)
}

class HSKWordView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {
    private var clickListener: OnHSKWordClickListener? = null
    private val bindings = HskWordViewBinding.inflate(
        LayoutInflater.from(context),
        this,
        true
    )

    init {
        bindings.root.setOnClickListener { clickListener?.onWordClick(this) }

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

    fun setOnWordClickListener(listener: OnHSKWordClickListener) {
        clickListener = listener
    }

    var pinyinText: String
        get() = bindings.pinyin.text.toString()
        set(value) {
            bindings.pinyin.text = value
        }

    var hanziText: String
        get() = bindings.hanzi.text.toString()
        set(value) {
            bindings.hanzi.text = value
        }

    var pinyinSize: Int
        get() = bindings.pinyin.textSize.toInt()
        set(value) {
            bindings.pinyin.textSize = value.toFloat()
        }

    var hanziSize: Int
        get() = bindings.hanzi.textSize.toInt()
        set(value) {
            bindings.hanzi.textSize = value.toFloat()
        }

    var pinyinColor: Int
        get() = bindings.pinyin.currentTextColor
        set(value) {
            bindings.pinyin.setTextColor(value)
        }

    var hanziColor: Int
        get() = bindings.hanzi.currentTextColor
        set(value) {
            bindings.hanzi.setTextColor(value)
        }

    var pinyinStyle: Int
        get() = bindings.pinyin.typeface.style
        set(value) {
            bindings.pinyin.setTypeface(null, value)
        }

    var hanziStyle: Int
        get() = bindings.hanzi.typeface.style
        set(value) {
            bindings.hanzi.setTypeface(null, value)
        }
}
