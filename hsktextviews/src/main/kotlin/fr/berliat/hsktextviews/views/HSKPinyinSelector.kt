package fr.berliat.hsktextviews.views

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.appcompat.widget.AppCompatSpinner
import androidx.core.content.ContextCompat

import fr.berliat.hsktextviews.R
import fr.berliat.pinyin4kot.Hanzi2Pinyin
import androidx.core.content.withStyledAttributes

interface HSKPinyinSelectorListener {
    fun onSelectPinyin(pinyin: String)
}

class HSKPinyinSelector @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.spinnerStyle
) : AppCompatSpinner(context, attrs, defStyleAttr, MODE_DROPDOWN) {
    private var listener: HSKPinyinSelectorListener? = null
    private val h2p = Hanzi2Pinyin()

    var pinyins : Array<String> = emptyArray()
        private set(value) {
            field = value // Update the backing field

            internalAdapter.clear()

            if (value.size > 1) {
                this.isEnabled = true
                this.isClickable = true
                background = ContextCompat.getDrawable(context, R.drawable.hsk_pinyin_selector_background)
            } else {
                this.isEnabled = false
                this.isClickable = false
                background = null
            }
            internalAdapter.addAll(value.toList())
        }

    var selectedPinyin : String? = null
        set(value) {
            val index = pinyins.indexOf(value)
            field = if (index == -1) { "" } else { value }
            setSelection(if (index == -1) { 0 } else { index })
        }

    var textSize: Int = 11
        set(value) {
            field = value
            internalAdapter.notifyDataSetChanged()
        }
    var textColor: Int = Color.DKGRAY
        set(value) {
            field = value
            internalAdapter.notifyDataSetChanged()
        }
    var textStyle: Int = Typeface.NORMAL
        set(value) {
            field = value
            internalAdapter.notifyDataSetChanged()
        }

    var hanzi: Char = ' '
        set(value) {
            field = value

            var newPinyins = mutableSetOf<String>()
            try {
                newPinyins = h2p.getPinyin(value).map { h2p.numberedToTonal(it) }.toMutableSet()
            } catch (_: Exception) {
                Log.e("HSKPinyinSelector", "Got asked for a seldom used HanZi")
            }

            if (autoAddFlatTones) {
                newPinyins = (newPinyins + newPinyins.map { h2p.pinyinToToneless(it) }).toMutableSet()
            }

            val newPinyinArray = newPinyins.toTypedArray()
            if (!pinyins.contentEquals(newPinyinArray)) // small optimization to avoid non needed redrawing
                pinyins = newPinyinArray
        }

    var autoAddFlatTones: Boolean = false
        set(value) {
            field = value
            hanzi = hanzi // trigger a restudy
        }

    // Without a layout, that we don't use, the
    val internalAdapter = object: ArrayAdapter<String>(context, android.R.layout.simple_spinner_item, mutableListOf<String>()) {
        private fun drawView(position: Int, convertView: View?): View {
            val textView = convertView as? TextView ?: TextView(context)
            textView.text = getItem(position)
            textView.setTextColor(textColor)
            textView.textSize = textSize.toFloat()
            textView.setPadding(0, 0, 0, 0)
            return textView
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            return drawView(position, convertView)
        }

        override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
            return drawView(position, convertView)
        }
    }

    init {
        onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                val text = (view as TextView).text.toString()
                selectedPinyin = text
                listener?.onSelectPinyin(view.text as String)
            }

            override fun onNothingSelected(parent: AdapterView<*>) { selectedPinyin = null }
        }

        attrs?.let {
            context.withStyledAttributes(it, R.styleable.HSKPinyinSelector, 0, 0) {
                hanzi = getString(R.styleable.HSKPinyinSelector_hanzi).toString()[0]
                textSize = getDimensionPixelSize(R.styleable.HSKPinyinSelector_textSize, DEFAULT_TEXT_SIZE)
                textStyle = getInt(R.styleable.HSKPinyinSelector_textStyle, DEFAULT_TEXT_STYLE)
                textColor = getColor(R.styleable.HSKPinyinSelector_textColor, DEFAULT_TEXT_COLOR)
                autoAddFlatTones = getBoolean(R.styleable.HSKPinyinSelector_autoAddFlatTones, DEFAULT_AUTO_ADD_FLAT_TONES)
            }
        }

        internalAdapter.addAll(pinyins.toList())

        isEnabled = false
        isClickable = false
        background = null
        adapter = internalAdapter
    }

    companion object {
        private const val DEFAULT_TEXT_SIZE = 27
        private const val DEFAULT_TEXT_STYLE = Typeface.NORMAL
        private const val DEFAULT_TEXT_COLOR = Color.DKGRAY
        private const val DEFAULT_AUTO_ADD_FLAT_TONES = false
    }
}
