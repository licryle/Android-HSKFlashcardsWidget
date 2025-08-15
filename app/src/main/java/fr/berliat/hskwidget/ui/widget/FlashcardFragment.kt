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
import fr.berliat.hskwidget.databinding.FlashcardWidgetNotConfiguredBinding

const val ARG_WIDGETID = "WIDGETID"
class FlashcardFragment : Fragment() {
    private var _widgetId: Int? = null
    private lateinit var bindings: FlashcardWidgetBinding
    private lateinit var notConfiguredBindings: FlashcardWidgetNotConfiguredBinding
    private lateinit var configuredBindings: FlashcardWidgetBinding
    private var isShowingConfiguredViews : Boolean? = null
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

        Log.i(TAG, "Creating a new Flashcard Fragment for widget $widgetId")

        _context = requireContext()
        _flashcardsMfr = FlashcardManager.getInstance(context, widgetId)
        flashcardsMfr.registerFragment(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        bindings = FlashcardWidgetBinding.inflate(layoutInflater)

        configuredBindings = FlashcardWidgetBinding.inflate(layoutInflater)
        notConfiguredBindings = FlashcardWidgetNotConfiguredBinding.inflate(layoutInflater)

        updateFlashcardView()

        return bindings.root
    }

    override fun onDestroy() {
        Log.i(TAG, "Destroying a new Flashcard Fragment")
        flashcardsMfr.deregisterFragment(this)

        super.onDestroy()
    }

    fun updateWord() {
        flashcardsMfr.updateWord()
    }

    fun updateFlashcardView() {
        lifecycleScope.launch {
            // Switch to the IO dispatcher to perform background work
            val currentWord = withContext(Dispatchers.IO) {
                flashcardsMfr.getCurrentWord()
            }

            // Switch back to the main thread to update UI
            withContext(Dispatchers.Main) {
                if (!::bindings.isInitialized) {
                    return@withContext
                }

                if (currentWord == null) {
                    Log.i(TAG, "Updating Fragment view, but no word available")

                    if (isShowingConfiguredViews != false) {
                        bindings.widgetRoot.removeAllViews()
                        bindings.widgetRoot.addView(notConfiguredBindings.root)
                        isShowingConfiguredViews = false
                    }
                } else {
                    Log.i(TAG, "Updating Fragment view with $currentWord")
                    if (isShowingConfiguredViews != true) {
                        bindings.widgetRoot.removeAllViews()
                        bindings.widgetRoot.addView(configuredBindings.root)
                        isShowingConfiguredViews = true
                    }

                    val openDictionary: () -> Unit = {
                        flashcardsMfr.openDictionary()
                    }

                    with(configuredBindings.flashcardChinese) {
                        setOnClickListener { openDictionary() }
                        text = currentWord.simplified
                    }

                    with(configuredBindings.flashcardDefinition) {
                        setOnClickListener { openDictionary() }
                        text = currentWord.definition[Locale.ENGLISH]
                    }

                    with(configuredBindings.flashcardPinyin) {
                        setOnClickListener { openDictionary() }
                        text = currentWord.pinyins.toString()
                    }

                    with(configuredBindings.flashcardHsklevel) {
                        visibility =
                            Utils.hideViewIf(currentWord.hskLevel == ChineseWord.HSK_Level.NOT_HSK)
                        setOnClickListener { openDictionary() }
                        text = currentWord.hskLevel.toString()
                    }

                    configuredBindings.flashcardSpeak.setOnClickListener {
                        flashcardsMfr.playWidgetWord()
                    }

                    configuredBindings.flashcardReload.setOnClickListener {
                        flashcardsMfr.updateWord()
                        Utils.logAnalyticsWidgetAction(
                            context,
                            Utils.ANALYTICS_EVENTS.WIDGET_MANUAL_WORD_CHANGE, widgetId
                        )
                    }

                    configuredBindings.root.invalidate()
                }
            }
        }
    }

    companion object {
        const val TAG = "FlashcardFragment"
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