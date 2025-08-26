package fr.berliat.hskwidget.domain

import android.util.Log
import com.huaban.analysis.jieba.JiebaSegmenter
import fr.berliat.hsktextviews.views.HSKTextView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class JiebaHSKTextSegmenter: HSKTextView.HSKTextSegmenter {
    override var listener: HSKTextView.HSKTextSegmenterListener? = null
    var segmenter: JiebaSegmenter? = null
        private set

    suspend fun preload() {
        var seg: JiebaSegmenter?

        withContext(Dispatchers.IO) {
            Log.d(TAG, "Loading JiebaSegmenter")
            seg = JiebaSegmenter()
            Log.d(TAG, "Finished Loading JiebaSegmenter")
        }

        withContext(Dispatchers.Main) {
            segmenter = seg
            listener?.onIsSegmenterReady()
        }
    }

    override fun isReady(): Boolean {
        return segmenter != null
    }

    override fun segment(text: String): Array<String>? {
        if (! isReady())
            return null
        else {
            val words = mutableListOf<String>()
            segmenter!!.process(text, JiebaSegmenter.SegMode.SEARCH).forEach {
                words.add(it.word.toString())
            }

            return words.toTypedArray()
        }
    }

    companion object {
        const val TAG = "HSKTextSegmenterJieba"
    }
}