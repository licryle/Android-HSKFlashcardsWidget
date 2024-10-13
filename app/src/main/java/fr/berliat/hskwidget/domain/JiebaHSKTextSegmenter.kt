package fr.berliat.hskwidget.domain

import android.util.Log
import com.huaban.analysis.jieba.JiebaSegmenter
import fr.berliat.hsktextviews.views.HSKTextView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class JiebaHSKTextSegmenter: HSKTextView.HSKTextSegmenter {
    var segmenter: JiebaSegmenter? = null
        protected set

    suspend fun preload() {
        var seg: JiebaSegmenter?
        withContext(Dispatchers.IO) {
            Log.d(TAG, "Loading JiebaSegmenter")
            seg = JiebaSegmenter()
            Log.d(TAG, "Finished Loading JiebaSegmenter")
        }

        withContext(Dispatchers.Main) {
            segmenter = seg
        }
    }

    override fun segment(text: String): Array<String>? {
        if (segmenter == null)
            return null
        else {
            val words = mutableListOf<String>()
            segmenter!!.process(text, JiebaSegmenter.SegMode.INDEX).forEach {
                words.add(it.word.toString())
            }

            return words.toTypedArray()
        }
    }

    companion object {
        const val TAG = "HSKTextSegmenterJieba"
    }
}