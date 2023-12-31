package fr.berliat.hskwidget.domain

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.util.Log
import fr.berliat.hskwidget.data.model.ChineseWord
import fr.berliat.hskwidget.data.store.ChineseWordsStore
import fr.berliat.hskwidget.data.store.FlashcardPreferencesStore
import fr.berliat.hskwidget.ui.flashcard.FlashcardFragment
import fr.berliat.hskwidget.ui.widget.FlashcardWidgetProvider
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async

class FlashcardManager private constructor(private val context: Context,
                                           private val widgetId: Int) {
    private val dict = ChineseWordsStore.getInstance(context)
    private val fragments = mutableMapOf<Int, MutableSet<FlashcardFragment>>()
    private val flashCardPrefs = getPreferenceStore()
    private val appWidgetMgr = AppWidgetManager.getInstance(context)

    fun getPreferenceStore(): FlashcardPreferencesStore {
        return FlashcardPreferencesStore(context, widgetId)
    }

    fun getCurrentWord() : ChineseWord {
        var currentWord = dict.findWordFromSimplified(flashCardPrefs.getCurrentSimplified())

        if (currentWord == null) currentWord = Utils.getDefaultWord(context)

        return currentWord
    }
    fun getNewWord(): ChineseWord {
        val currentWord = dict.findWordFromSimplified(flashCardPrefs.getCurrentSimplified())

        var newWord = dict.getRandomWord(flashCardPrefs.getAllowedHSK(), arrayListOf(currentWord!!))

        if (newWord == null) newWord = Utils.getDefaultWord(context)

        // Persist it in preferences for cross-App convenience
        flashCardPrefs.putCurrentSimplified(newWord.simplified)

        return newWord
    }

    fun updateWord() {
        Log.i("FlashcardManager", "Word update requested")
        getNewWord()

        Log.i("FlashcardManager", "Now calling for fragments' update")
        if (fragments[widgetId] != null)
            fragments[widgetId]!!.forEach{it.updateFlashcardView() }

        Log.i("FlashcardManager", "Now calling for widgets' update")
        GlobalScope.async {
            FlashcardWidgetProvider().updateFlashCardWidget(context, appWidgetMgr, widgetId)
        }
    }

    fun registerFragment(frag: FlashcardFragment) : Boolean {
        if (fragments[widgetId] == null) fragments[widgetId] = mutableSetOf()

        return fragments[widgetId]!!.add(frag)
    }

    fun deregisterFragment(frag: FlashcardFragment) : Boolean{
        if (fragments[widgetId] == null) return false

        return fragments[widgetId]!!.remove(frag)
    }

    fun playWidgetWord() : Boolean {
        val word = flashCardPrefs.getCurrentSimplified()

        if (word == "") return false

        Utils.playWordInBackground(context, word)

        return true
    }

    fun openDictionary() {
        val word = flashCardPrefs.getCurrentSimplified()

        context.startActivity(getOpenDictionaryIntent(word))

        Utils.logAnalyticsWidgetAction(
            context,
            Utils.ANALYTICS_EVENTS.WIDGET_OPEN_DICTIONARY, widgetId
        )
    }

    fun getOpenDictionaryIntent(word: String): Intent {
        return Utils.getOpenURLIntent("https://www.wordsense.eu/$word/")
    }

    companion object {
        private var instances = mutableMapOf<Int, FlashcardManager>()

        fun getInstance(context: Context, widgetId: Int) =
            instances[widgetId] ?: synchronized(this) {
                instances[widgetId] ?: FlashcardManager(context, widgetId)
                    .also { instances[widgetId] = it }
            }
    }
}