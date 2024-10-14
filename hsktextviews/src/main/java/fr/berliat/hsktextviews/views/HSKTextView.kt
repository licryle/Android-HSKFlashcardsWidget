package fr.berliat.hsktextviews.views

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import androidx.recyclerview.widget.RecyclerView
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayout
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent
import fr.berliat.hsktextviews.R
import fr.berliat.hsktextviews.databinding.HskTextViewBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.ceil


class HSKTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : RecyclerView(context, attrs, defStyleAttr), OnHSKWordClickListener {
    interface HSKTextSegmenter {
        var listener: HSKTextSegmenterListener?

        fun segment(text: String): Array<String>?
        fun isReady(): Boolean
    }

    interface HSKTextSegmenterListener {
        fun onIsSegmenterReady()
    }

    interface HSKTextListener {
        fun onWordClick(word: HSKWordView)
        fun onTextAnalysisStart()
        fun onTextAnalysisSuccess()
        fun onTextAnalysisFailure(e: Error)
    }

    var listener: HSKTextListener? = null
    var segmenter: HSKTextSegmenter? = null

    private val wordsAdapter: HSKWordsAdapter
    private var originalText: String = ""

    private val DEFAULT_HANZI_SIZE = 20
    private val DEFAULT_WORD_SEPARATOR = ""

    private val DEFAULT_CLICKED_BG_COLOR = Color.BLACK
    private val DEFAULT_CLICKED_TXT_COLOR = Color.WHITE

    init {
        val loManager = FlexboxLayoutManager(context)
        loManager.flexDirection = FlexDirection.ROW
        loManager.flexWrap = FlexWrap.WRAP
        loManager.justifyContent = JustifyContent.FLEX_START
        layoutManager = loManager

        wordsAdapter = HSKWordsAdapter(context, this)
        adapter = wordsAdapter

        HskTextViewBinding.inflate(
            LayoutInflater.from(context),
            this,
            false
        )

        attrs?.let {
            val typedArray = context.obtainStyledAttributes(it, R.styleable.HSKTextView, 0, 0)

            text = typedArray.getString(R.styleable.HSKTextView_text) ?: ""
            wordSeparator = typedArray.getString(R.styleable.HSKTextView_wordSeparator) ?: ""
            hanziTextSize = typedArray.getDimensionPixelSize(R.styleable.HSKTextView_hanziTextSize, wordsAdapter.hanziSize)

            clickedBackgroundColor = typedArray.getColor(R.styleable.HSKTextView_clickedWordBackgroundColor, Color.BLACK)
            clickedHanziColor = typedArray.getColor(R.styleable.HSKTextView_clickedWordHanziColor, Color.WHITE)
            clickedPinyinColor = typedArray.getColor(R.styleable.HSKTextView_clickedWordPinyinColor, Color.WHITE)

            typedArray.recycle()
        }
    }

    var clickedWords: MutableMap<String, String> = mutableMapOf()
        set(value) {
            wordsAdapter.clickedWords = value
        }

    var wordSeparator: String
        get() = wordsAdapter.wordSeparator
        set(value) {
            wordsAdapter.wordSeparator = value
        }

    var hanziTextSize: Int
        get() = wordsAdapter.hanziSize
        set(value) {
            if (value < 8)
                throw UnsupportedOperationException("Text Too Small")
            else {
                wordsAdapter.hanziSize = value
            }
        }

    var displayPinYin: Boolean
        get() = false
        set(value) {
            if (value)
                throw UnsupportedOperationException("Not implemented yet")
        }

    var clickedBackgroundColor: Int
        get() = wordsAdapter.clickedBackgroundColor
        set(value) {
            wordsAdapter.clickedBackgroundColor = value
        }

    var clickedHanziColor: Int
        get() = wordsAdapter.clickedHanziColor
        set(value) {
            wordsAdapter.clickedHanziColor = value
        }

    var clickedPinyinColor: Int
        get() = wordsAdapter.clickedPinyinColor
        set(value) {
            wordsAdapter.clickedPinyinColor = value
        }

    var text: String
        get() = originalText
        set(value) {
            var cleanText = value.trim().replace("\\s+".toRegex(), " ")
            cleanText = cleanText.replace("(\\n|\\n\\r)".toRegex(), "\n")

            originalText = cleanText

            if (originalText != "") {
                listener?.onTextAnalysisStart()

                GlobalScope.launch {
                    Log.d(TAG, "Start parsing")

                    val words = mutableListOf<Pair<String, String>>()

                    cleanText.split("\n").forEach { paragraph ->
                        segmenter?.segment(paragraph)?.forEach { word ->
                            words.add(Pair(word, ""))
                        }
                        words.add(Pair("\n", ""))
                    }

                    Log.d(TAG, "Finished parsing")
                    withContext(Dispatchers.Main) {
                        if (words.size > 1)
                            listener?.onTextAnalysisSuccess()
                        else
                            listener?.onTextAnalysisFailure(Error("Segmenter is null"))

                        Log.d(TAG, "Start rendering parsing")
                        wordsAdapter.addData(words)
                        Log.d(TAG, "Finished rendering parsing")
                    }
                }
            }
        }

    private fun hanziToPinyin(hanzi: String): String {
        // @todo(Licryle): look up pinyin
        return if (displayPinYin) {
            "hanzi"
        } else {
            ""
        }
    }

    override fun onWordClick(wordView: HSKWordView) {
        listener?.onWordClick(wordView)
    }

    class HSKWordsHolder(private val wordView: HSKWordView,
                         private val listener: OnHSKWordClickListener)
        : ViewHolder(wordView.rootView) {

        fun bind(word: Pair<String, String>, hanziSize: Int, wordSeparator: String,
                 clickedBackgroundColor: Int, clickedHanziColor: Int, clickedPinyinColor: Int,
                 isClicked: Boolean) {
            wordView.hanziText = word.first
            wordView.hanziSize = hanziSize
            wordView.pinyinText = word.second
            wordView.pinyinSize = ceil((hanziSize * 3 / 4).toDouble()).toInt()
            wordView.endSeparator = wordSeparator
            wordView.clickedBackgroundColor = clickedBackgroundColor
            wordView.clickedHanziColor = clickedHanziColor
            wordView.clickedPinyinColor = clickedPinyinColor
            wordView.isClicked = isClicked

            var layoutParams = FlexboxLayoutManager.LayoutParams(
                FlexboxLayout.LayoutParams.WRAP_CONTENT,
                FlexboxLayout.LayoutParams.WRAP_CONTENT
            )

            if (word.first == "\n") {
                layoutParams = FlexboxLayoutManager.LayoutParams(
                    FlexboxLayout.LayoutParams.MATCH_PARENT,
                    5 // Height of 0 to act as a spacer
                ).apply {
                    flexGrow = 1f // Allow it to grow and push items down
                }
                wordView.setBackgroundColor(Color.RED)
            } else {
                wordView.setOnWordClickListener(listener)
            }

            wordView.rootView.layoutParams = layoutParams
        }
    }

    companion object {
        const val TAG = "HSKTextView"
    }
}