package fr.berliat.hsktextviews.views

import android.annotation.SuppressLint
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

            endSeparator = typedArray.getString(R.styleable.HSKWordView_endSeparator) ?: ""

            isClicked = typedArray.getBoolean(R.styleable.HSKWordView_isClicked, false)
            clickedBackgroundColor = typedArray.getColor(R.styleable.HSKWordView_clickedBackgroundColor, Color.BLACK)
            clickedHanziColor = typedArray.getColor(R.styleable.HSKWordView_clickedHanziColor, Color.WHITE)
            clickedPinyinColor = typedArray.getColor(R.styleable.HSKWordView_clickedPinyinColor, Color.WHITE)

            typedArray.recycle()
        }
    }


    @SuppressLint("SetTextI18n")
    private fun updateHanziText() {
        bindings.hanzi.text = hanziText + endSeparator
    }

    var pinyinText: String
        get() = bindings.pinyin.text.toString()
        set(value) {
            bindings.pinyin.text = value
        }

    var hanziText: String = ""
        set(value) {
            field = value
            updateHanziText()
        }

    var endSeparator: String = ""
        set(value) {
            field = value
            updateHanziText()
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

    var pinyinColor: Int = Color.DKGRAY
        set(value) {
            field = value
            if (!isClicked)
                bindings.pinyin.setTextColor(value)
        }

    var hanziColor: Int = Color.BLACK
        set(value) {
            field = value
            if (!isClicked)
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

    var isClicked: Boolean = false
        set(value) {
            field = value

            if (value) {
                setBackgroundColor(clickedBackgroundColor)
                bindings.pinyin.setTextColor(clickedPinyinColor)
                bindings.hanzi.setTextColor(clickedHanziColor)
            } else {
                setBackgroundColor(Color.TRANSPARENT)
                bindings.pinyin.setTextColor(pinyinColor)
                bindings.hanzi.setTextColor(hanziColor)
            }
        }

    var clickedBackgroundColor: Int = Color.BLACK
        set(value) {
            field = value
            if (isClicked)
                bindings.root.setBackgroundColor(value)
        }

    var clickedHanziColor: Int = Color.WHITE
        set(value) {
            field = value
            if (isClicked)
                bindings.hanzi.setTextColor(value)
        }

    var clickedPinyinColor: Int = Color.WHITE
        set(value) {
            field = value
            if (isClicked)
                bindings.pinyin.setTextColor(value)
        }
    fun setOnWordClickListener(listener: HSKWordClickListener) {
        clickListener = listener
    }
}
