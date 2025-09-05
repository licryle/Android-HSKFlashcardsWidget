package fr.berliat.hsktextviews.views

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.util.AttributeSet
import android.util.TypedValue
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.core.view.children
import androidx.core.view.setPadding

import fr.berliat.hsktextviews.R
import fr.berliat.hsktextviews.databinding.HskWordViewBinding
import androidx.core.content.withStyledAttributes

interface HSKWordClickListener {
    fun onWordClick(wordView: HSKWordView)
}

class HSKWordView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {
    private var clickListener: HSKWordClickListener? = null
    private val bindings = HskWordViewBinding.inflate(
        LayoutInflater.from(context),
        this,
        true
    )

    var pinyinText: String
        get() {
            if (pinyinEditable) {
                val pinyins = mutableListOf<String?>()
                bindings.pinyinEditor.children.forEach {
                    pinyins.add((it as HSKPinyinSelector).selectedPinyin)
                }

                return pinyins.filter { ! it.isNullOrEmpty() }.joinToString(" ")
            } else {
                return bindings.pinyinDisplay.text.toString()
            }
        }
        set(value) {
            bindings.pinyinDisplay.text = value
            getPinyinSelectors().forEach { it.selectedPinyin = value }
        }

    var hanziText: String = ""
        set(value) {
            field = value
            updateHanziText()
        }

    var endSeparator: String = DEFAULT_END_SEPARATOR
        set(value) {
            field = value
            updateHanziText()
        }

    var _pinyinSize : Int = DEFAULT_PINYIN_SIZE
    var pinyinSize: Int
        get() = _pinyinSize
        set(value) {
            _pinyinSize = value
            bindings.pinyinDisplay.textSize = _pinyinSize.toFloat()
            getPinyinSelectors().forEach { it.textSize = _pinyinSize }
        }

    var hanziSize: Int
        get() = bindings.hanzi.textSize.toInt()
        set(value) {
            bindings.hanzi.textSize = value.toFloat()
        }

    var pinyinColor: Int = DEFAULT_PINYIN_COLOR
        set(value) {
            field = value
            if (!isClicked)
                bindings.pinyinDisplay.setTextColor(value)

            getPinyinSelectors().forEach { it.textColor = value }
        }

    var hanziColor: Int = DEFAULT_HANZI_COLOR
        set(value) {
            field = value
            if (!isClicked)
                bindings.hanzi.setTextColor(value)
        }

    var pinyinStyle: Int
        get() = bindings.pinyinDisplay.typeface?.style ?: DEFAULT_PINYIN_STYLE
        set(value) {
            bindings.pinyinDisplay.setTypeface(null, value)

            getPinyinSelectors().forEach { it.textStyle = value }
        }

    var hanziStyle: Int
        get() = bindings.hanzi.typeface.style
        set(value) {
            bindings.hanzi.setTypeface(null, value)
        }

    var pinyinEditable: Boolean = DEFAULT_PINYIN_EDITABLE
        set(value) {
            val displayedPinyin = pinyinText // before changing the value so we collect from the Display
            field = value
            recreatePinyinSelectors(displayedPinyin)
            if (value) {
                bindings.pinyinDisplay.visibility = GONE
                bindings.pinyinEditor.visibility = VISIBLE
            } else {
                bindings.pinyinDisplay.visibility = VISIBLE
                bindings.pinyinEditor.visibility = GONE
            }
        }

    var isClicked: Boolean = DEFAULT_IS_CLICKED
        set(value) {
            field = value

            if (value) {
                setBackgroundColor(clickedBackgroundColor)
                bindings.pinyinDisplay.setTextColor(clickedPinyinColor)
                bindings.hanzi.setTextColor(clickedHanziColor)
            } else {
                setBackgroundColor(Color.TRANSPARENT)
                bindings.pinyinDisplay.setTextColor(pinyinColor)
                bindings.hanzi.setTextColor(hanziColor)
            }
        }

    var clickedBackgroundColor: Int = DEFAULT_CLICKED_BG_COLOR
        set(value) {
            field = value
            if (isClicked)
                bindings.root.setBackgroundColor(value)
        }

    var clickedHanziColor: Int = DEFAULT_HANZI_COLOR
        set(value) {
            field = value
            if (isClicked)
                bindings.hanzi.setTextColor(value)
        }

    var clickedPinyinColor: Int = DEFAULT_PINYIN_COLOR
        set(value) {
            field = value
            if (isClicked)
                bindings.pinyinDisplay.setTextColor(value)
        }

    init {
        bindings.root.setOnClickListener { clickListener?.onWordClick(this) }

        attrs?.let {
            context.withStyledAttributes(it, R.styleable.HSKWordView, 0, 0) {
                pinyinText = getString(R.styleable.HSKWordView_pinyin) ?: ""
                hanziText = getString(R.styleable.HSKWordView_hanzi) ?: ""

                pinyinSize = getDimensionPixelSize(R.styleable.HSKWordView_pinyinSize, _pinyinSize)
                hanziSize = getDimensionPixelSize(R.styleable.HSKWordView_hanziSize, DEFAULT_HANZI_SIZE)

                pinyinColor = getColor(R.styleable.HSKWordView_pinyinColor, DEFAULT_PINYIN_COLOR)
                hanziColor = getColor(R.styleable.HSKWordView_hanziColor, DEFAULT_HANZI_COLOR)

                pinyinStyle = getInt(R.styleable.HSKWordView_pinyinStyle, DEFAULT_PINYIN_STYLE)
                hanziStyle = getInt(R.styleable.HSKWordView_hanziStyle, DEFAULT_HANZI_STYLE)

                endSeparator = getString(R.styleable.HSKWordView_endSeparator) ?: DEFAULT_END_SEPARATOR

                isClicked = getBoolean(R.styleable.HSKWordView_isClicked, DEFAULT_IS_CLICKED)
                clickedBackgroundColor =
                    getColor(R.styleable.HSKWordView_clickedBackgroundColor, DEFAULT_CLICKED_BG_COLOR)
                clickedHanziColor = getColor(R.styleable.HSKWordView_clickedHanziColor, DEFAULT_CLICKED_PINYIN_COLOR)
                clickedPinyinColor =
                    getColor(R.styleable.HSKWordView_clickedPinyinColor, DEFAULT_CLICKED_HANZI_COLOR)

                pinyinEditable = getBoolean(R.styleable.HSKWordView_pinyinEditable, DEFAULT_PINYIN_EDITABLE)
            }
        }
    }

    fun setOnWordClickListener(listener: HSKWordClickListener) {
        clickListener = listener
    }

    private fun createPinyinSelector(h: Char, selectedPinyin: String, includeFlatTone: Boolean)
        : HSKPinyinSelector {
        val selector = HSKPinyinSelector(context)
        selector.autoAddFlatTones = includeFlatTone
        selector.hanzi = h
        selector.selectedPinyin = selectedPinyin
        selector.textSize = (pinyinSize * 0.90).toInt() // to account for dropdown arrow
        selector.textStyle = pinyinStyle
        selector.textColor = pinyinColor

        val rightMargin = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            5f, // sp value
            context.resources.displayMetrics
        ).toInt()

        val params = LayoutParams(
            LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT
        )

        // Set margins in pixels
        params.setMargins(
            0,   // left
            0,   // top
            rightMargin,   // right
            0    // bottom
        )

        // Apply LayoutParams
        selector.layoutParams = params

        // Set padding inside spinner for text
        selector.setPadding(0)

        return selector
    }

    private fun recreatePinyinSelectors(pinyin: String) {
        if (! pinyinEditable) return

        bindings.pinyinEditor.removeAllViews()

        val pinyins = pinyin.split(" ")

        hanziText.forEachIndexed { i, hanzi ->
            val includeFlatTone = i == (hanziText.length - 1)
            val selector = createPinyinSelector(
                hanzi,
                pinyins.getOrElse(i) { "" },
                includeFlatTone)

            bindings.pinyinEditor.addView(selector)
        }
    }

    private fun getPinyinSelectors() : List<HSKPinyinSelector> {
        return bindings.pinyinEditor.children
            .filterIsInstance<HSKPinyinSelector>()
            .toList()
    }

    @SuppressLint("SetTextI18n")
    private fun updateHanziText() {
        bindings.hanzi.text = hanziText + endSeparator
        recreatePinyinSelectors(pinyinText)
    }

    companion object {
        private const val DEFAULT_PINYIN_SIZE = 27
        private const val DEFAULT_PINYIN_STYLE = Typeface.NORMAL
        private const val DEFAULT_PINYIN_COLOR = Color.DKGRAY
        private const val DEFAULT_END_SEPARATOR = ""
        private const val DEFAULT_HANZI_SIZE = 36
        private const val DEFAULT_HANZI_STYLE = Typeface.BOLD
        private const val DEFAULT_HANZI_COLOR = Color.BLACK
        private const val DEFAULT_IS_CLICKED = false
        private const val DEFAULT_CLICKED_BG_COLOR = Color.BLACK
        private const val DEFAULT_CLICKED_PINYIN_COLOR = Color.WHITE
        private const val DEFAULT_CLICKED_HANZI_COLOR = Color.WHITE
        private const val DEFAULT_PINYIN_EDITABLE = false
    }
}
