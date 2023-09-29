package fr.berliat.hskwidget.ui.widgets

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import fr.berliat.hskwidget.R
import fr.berliat.hskwidget.domain.FlashcardManager
import java.util.Locale

private const val ARG_WIDGETID = "WIDGETID"

class FlashcardFragment : Fragment() {
    private var _widgetId: Int? = null
    private var _root : View? = null
    private var _flashcardsMfr : FlashcardManager? = null

    // Properties only valid between onCreateView and onDestroyView.
    private val widgetId get() = _widgetId!!
    private val root get() = _root!!
    private val flashcardsMfr get() = _flashcardsMfr!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            _widgetId = it.getInt(ARG_WIDGETID)
        }

        _flashcardsMfr = FlashcardManager.getInstance(requireContext(), widgetId)
        flashcardsMfr.registerFragment(this)
    }

    override fun onDestroy() {
        flashcardsMfr.deregisterFragment(this)

        super.onDestroy()
    }

    fun updateFlashcardView() {
        val currentWord = flashcardsMfr.getCurrentWord()
        Log.i("WidgetsWidgetFragment", "Updating Fragment view with $currentWord")

        with(root.findViewById<TextView>(R.id.flashcard_chinese)) {
            setOnClickListener{ flashcardsMfr.openDictionary() }
            text = currentWord.simplified
        }

        with(root.findViewById<TextView>(R.id.flashcard_definition)) {
            setOnClickListener{ flashcardsMfr.openDictionary() }
            text = currentWord.definition[Locale.ENGLISH]
        }

        with(root.findViewById<TextView>(R.id.flashcard_pinyin)) {
            setOnClickListener{ flashcardsMfr.openDictionary() }
            text = currentWord.pinyins.toString()
        }

        with(root.findViewById<TextView>(R.id.flashcard_hsklevel)) {
            setOnClickListener{ flashcardsMfr.openDictionary() }
            text = currentWord.HSK.toString()
        }

        root.findViewById<View>(R.id.flashcard_speak).setOnClickListener{
            flashcardsMfr.playWidgetWord()
        }

        root.findViewById<View>(R.id.flashcard_reload).setOnClickListener{
            flashcardsMfr.updateWord()
        }

        root.invalidate()
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
         * @return A new instance of fragment WidgetsWidgetFragment.
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