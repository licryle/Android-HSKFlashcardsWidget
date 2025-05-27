package fr.berliat.hskwidget.ui.widget

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import fr.berliat.hskwidget.data.model.ChineseWord
import fr.berliat.hskwidget.domain.FlashcardManager
import fr.berliat.hskwidget.domain.Utils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

import fr.berliat.hskwidget.databinding.FlashcardWidgetBinding

const val ARG_WIDGETID = "WIDGETID"
class FlashcardFragment : Fragment() {
    private var _widgetId: Int? = null
    private lateinit var bindings: FlashcardWidgetBinding
    private var _flashcardsMfr: FlashcardManager? = null
    private var _context: Context? = null

    // Properties only valid between onCreateView and onDestroyView.
    private val widgetId get() = _widgetId!!
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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        bindings = FlashcardWidgetBinding.inflate(layoutInflater)

        updateFlashcardView()

        return bindings.root
    }

    override fun onDestroy() {
        Log.i("FlashcardFragment", "Destroying a new Flashcard Fragment")
        flashcardsMfr.deregisterFragment(this)

        super.onDestroy()
    }

    fun updateFlashcardView() {
        lifecycleScope.launch {
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

                with(bindings.flashcardChinese) {
                    setOnClickListener { openDictionary() }
                    text = currentWord.simplified
                }

                with(bindings.flashcardDefinition) {
                    setOnClickListener { openDictionary() }
                    text = currentWord.definition[Locale.ENGLISH]
                }

                with(bindings.flashcardPinyin) {
                    setOnClickListener { openDictionary() }
                    text = currentWord.pinyins.toString()
                }

                with(bindings.flashcardHsklevel) {
                    visibility = Utils.hideViewIf(currentWord.hskLevel == ChineseWord.HSK_Level.NOT_HSK)
                    setOnClickListener { openDictionary() }
                    text = currentWord.hskLevel.toString()
                }

                bindings.flashcardSpeak.setOnClickListener{
                    flashcardsMfr.playWidgetWord()
                    Utils.logAnalyticsWidgetAction(
                        context,
                        Utils.ANALYTICS_EVENTS.WIDGET_PLAY_WORD, widgetId
                    )
                }

                bindings.flashcardReload.setOnClickListener{
                    flashcardsMfr.updateWord()
                    Utils.logAnalyticsWidgetAction(
                        context,
                        Utils.ANALYTICS_EVENTS.WIDGET_MANUAL_WORD_CHANGE, widgetId
                    )
                }

                bindings.root.invalidate()
            }
        }
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