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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.ceil
import androidx.core.content.withStyledAttributes

import fr.berliat.pinyin4kot.Hanzi2Pinyin
import kotlin.text.forEach

class HSKTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : RecyclerView(context, attrs, defStyleAttr), HSKWordClickListener {
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

    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

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
            context.withStyledAttributes(it, R.styleable.HSKTextView, 0, 0) {
                text = getString(R.styleable.HSKTextView_text) ?: ""
                wordSeparator = getString(R.styleable.HSKTextView_wordSeparator) ?: DEFAULT_WORD_SEPARATOR
                hanziTextSize = getDimensionPixelSize(
                    R.styleable.HSKTextView_hanziTextSize,
                    wordsAdapter.hanziSize
                )
                pinyinColor =
                    getColor(R.styleable.HSKTextView_pinyinColor, DEFAULT_PINYIN_COLOR)
                hanziColor =
                    getColor(R.styleable.HSKTextView_hanziColor, DEFAULT_HANZI_COLOR)

                clickedBackgroundColor =
                    getColor(R.styleable.HSKTextView_clickedWordBackgroundColor, DEFAULT_CLICKED_BG_COLOR)
                clickedHanziColor =
                    getColor(R.styleable.HSKTextView_clickedWordHanziColor, DEFAULT_CLICKED_TXT_COLOR)
                clickedPinyinColor =
                    getColor(R.styleable.HSKTextView_clickedWordPinyinColor, DEFAULT_CLICKED_TXT_COLOR)

                showPinyins = HSKWordsAdapter.ShowPinyins.fromInt(getInt(R.styleable.HSKTextView_showPinyins, DEFAULT_SHOW_PINYINS))
            }
        }
    }

    var clickedWords: MutableMap<String, String> = mutableMapOf()
        set(value) {
            wordsAdapter.clickedWords = value
        }

    var showPinyins: HSKWordsAdapter.ShowPinyins
        get() = wordsAdapter.showPinyins
        set(value) {
            wordsAdapter.showPinyins = value
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

    var pinyinColor: Int
        get() = wordsAdapter.pinyinColor
        set(value) { wordsAdapter.pinyinColor = value }

    var hanziColor: Int
        get() = wordsAdapter.hanziColor
        set(value) { wordsAdapter.hanziColor = value }

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

            if (cleanText != "") {
                listener?.onTextAnalysisStart()

                coroutineScope.launch(Dispatchers.IO) {
                    Log.d(TAG, "Start parsing")

                    val words = mutableListOf<Pair<String, String>>()

                    cleanText.split("\n").forEach { paragraph ->
                        segmenter?.segment(paragraph)?.forEach { word ->
                            words.add(Pair(word, inferPinyin(word)))
                        }
                        words.add(Pair("\n", ""))
                    }

                    Log.d(TAG, "Finished parsing")
                    withContext(Dispatchers.Main) {
                        Log.d(TAG, "Start rendering parsing")
                        wordsAdapter.clearData()
                        wordsAdapter.addData(words)
                        Log.d(TAG, "Finished rendering parsing")

                        if (words.size > 1)
                            listener?.onTextAnalysisSuccess()
                        else
                            listener?.onTextAnalysisFailure(Error("Segmenter is null"))
                    }
                }
            }
        }

    val wordsFrequency: Map<String, Int>
        get() = wordsAdapter.wordsFrequency

    override fun onWordClick(wordView: HSKWordView) {
        listener?.onWordClick(wordView)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        // Clean up any running coroutines to avoid memory leaks
        coroutineScope.cancel()
    }

    fun inferPinyin(word: String): String {
        val pinyins = StringBuilder()
        try {
            word.forEach { hanzi ->
                val py = h2p.getPinyin(hanzi)[0]
                pinyins.append(h2p.numberedToTonal(py)).append(" ")
            }
        } catch (_: Exception) {
            // Not a correct or known pinyin
            pinyins.append("   ")
        }

        return pinyins.toString()
    }

    class HSKWordsHolder(private val wordView: HSKWordView,
                         private val listener: HSKWordClickListener)
        : ViewHolder(wordView.rootView) {

        fun bind(word: Pair<String, String>, hanziSize: Int, hanziColor: Int, pinyinColor: Int,
                 wordSeparator: String, clickedBackgroundColor: Int, clickedHanziColor: Int,
                 clickedPinyinColor: Int, isClicked: Boolean, showPinyin: Boolean) {
            wordView.hanziText = word.first
            wordView.hanziSize = hanziSize
            wordView.hanziColor = hanziColor
            wordView.pinyinText = if (showPinyin) word.second else ""
            wordView.pinyinSize = ceil((hanziSize * 3 / 4).toDouble()).toInt()
            wordView.pinyinColor = pinyinColor
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
            } else if (containsChinese(word.first)) {
                wordView.setOnWordClickListener(listener)
            }

            wordView.rootView.layoutParams = layoutParams
        }
    }

    companion object {
        val h2p = Hanzi2Pinyin() // Tiny optim to keep in memory
        const val TAG = "HSKTextView"

        private const val DEFAULT_WORD_SEPARATOR = ""
        private const val DEFAULT_CLICKED_BG_COLOR = Color.BLACK
        private const val DEFAULT_CLICKED_TXT_COLOR = Color.WHITE
        private const val DEFAULT_HANZI_COLOR = Color.BLACK
        private const val DEFAULT_PINYIN_COLOR = Color.DKGRAY
        private val DEFAULT_SHOW_PINYINS = HSKWordsAdapter.ShowPinyins.ALL.value

        fun containsChinese(text: String): Boolean {
            val pattern = Regex("[\u4e00-\u9fff]")
            return pattern.containsMatchIn(text)
        }
    }
}