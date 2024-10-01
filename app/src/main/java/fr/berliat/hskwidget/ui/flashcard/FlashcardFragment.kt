package fr.berliat.hskwidget.ui.flashcard

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import fr.berliat.hskwidget.R
import fr.berliat.hskwidget.domain.FlashcardManager
import fr.berliat.hskwidget.domain.Utils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

const val ARG_WIDGETID = "WIDGETID"
class FlashcardFragment : Fragment() {
    private var _widgetId: Int? = null
    private var _root: View? = null
    private var _flashcardsMfr: FlashcardManager? = null
    private var _context: Context? = null

    // Properties only valid between onCreateView and onDestroyView.
    private val widgetId get() = _widgetId!!
    private val root get() = _root!!
    private val flashcardsMfr get() = _flashcardsMfr!!

    @get:JvmName("getContext2")
    private val context get() = _context!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            _widgetId = it.getInt(ARG_WIDGETID)
        }

        Log.i("FlashcardFragment", "Creating a new Flashcard Fragment for widget $widgetId")

        _context = requireContext()
        _flashcardsMfr = FlashcardManager.getInstance(context, widgetId)
        flashcardsMfr.registerFragment(this)
    }

    override fun onDestroy() {
        Log.i("FlashcardFragment", "Destroying a new Flashcard Fragment")
        flashcardsMfr.deregisterFragment(this)

        super.onDestroy()
    }

    fun updateFlashcardView() {
        GlobalScope.launch {
            // Switch to the IO dispatcher to perform background work
            val currentWord = withContext(Dispatchers.IO) {
                flashcardsMfr.getCurrentWord()
            }

            // Switch back to the main thread to update UI
            withContext(Dispatchers.Main) {
                Log.i("FlashcardFragment", "Updating Fragment view with $currentWord")

                val openDictionary: () -> Unit = {
                    flashcardsMfr.openDictionary()
                }

                with(root.findViewById<TextView>(R.id.flashcard_chinese)) {
                    setOnClickListener { openDictionary() }
                    text = currentWord.simplified
                }

                with(root.findViewById<TextView>(R.id.flashcard_definition)) {
                    setOnClickListener { openDictionary() }
                    text = currentWord.definition[Locale.ENGLISH]
                }

                with(root.findViewById<TextView>(R.id.flashcard_pinyin)) {
                    setOnClickListener { openDictionary() }
                    text = currentWord.pinyins.toString()
                }

                with(root.findViewById<TextView>(R.id.flashcard_hsklevel)) {
                    setOnClickListener { openDictionary() }
                    text = currentWord.hskLevel.toString()
                }

                root.findViewById<View>(R.id.flashcard_speak).setOnClickListener{
                    flashcardsMfr.playWidgetWord()
                    Utils.logAnalyticsWidgetAction(
                        context,
                        Utils.ANALYTICS_EVENTS.WIDGET_PLAY_WORD, widgetId
                    )
                }

                root.findViewById<View>(R.id.flashcard_reload).setOnClickListener{
                    flashcardsMfr.updateWord()
                    Utils.logAnalyticsWidgetAction(
                        context,
                        Utils.ANALYTICS_EVENTS.WIDGET_MANUAL_WORD_CHANGE, widgetId
                    )
                }

                root.invalidate()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _root = inflater.inflate(R.layout.flashcard_widget, container, false)

        updateFlashcardView()

        return root
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param widgetId The id the widget to configure
         * @return A new instance of fragment WidgetsWidgetConfPreviewFragment.
         */
        @JvmStatic
        fun newInstance(widgetId: Int) =
            FlashcardFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_WIDGETID, widgetId)
                }
            }
    }
}