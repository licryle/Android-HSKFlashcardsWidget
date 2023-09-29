package fr.berliat.hskwidget.domain

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import fr.berliat.hskwidget.data.model.ChineseWord
import fr.berliat.hskwidget.data.store.ChineseWordsStore
import fr.berliat.hskwidget.ui.widget.FlashcardWidget
import fr.berliat.hskwidget.ui.widget.getWidgetPreferences
import fr.berliat.hskwidget.ui.widgets.FlashcardFragment

class FlashcardManager private constructor(private val context: Context,
                                           private val widgetId: Int) {
    private val dict = ChineseWordsStore.getInstance(context)
    private val fragments = mutableMapOf<Int, MutableSet<FlashcardFragment>>()

    fun getCurrentWord() : ChineseWord {
        val preferences = getWidgetPreferences(context, widgetId)
        var currentWord = dict.findWordFromSimplified(preferences.getCurrentSimplified())

        if (currentWord == null) currentWord = Utils.getDefaultWord(context)

        return currentWord
    }
    fun getNewWord(): ChineseWord {
        val preferences = getWidgetPreferences(context, widgetId)
        val currentWord = dict.findWordFromSimplified(preferences.getCurrentSimplified())

        var newWord = dict.getRandomWord(preferences.getAllowedHSK(), arrayListOf(currentWord!!))

        if (newWord == null) newWord = Utils.getDefaultWord(context)

        // Persist it in preferences for cross-App convenience
        preferences.putCurrentSimplified(newWord.simplified)

        return newWord
    }

    fun updateWord() {
        Log.i("FlashcardManager", "Word update requested")
        getNewWord()

        Log.i("FlashcardManager", "Now calling for fragments' update")
        if (fragments[widgetId] != null)
            fragments[widgetId]!!.forEach{it.updateFlashcardView() }

        Log.i("FlashcardManager", "Now calling for widgets' update")
        FlashcardWidget().updateFlashCardWidget(context,
            AppWidgetManager.getInstance(context), widgetId)
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
        val word = getWidgetPreferences(context, widgetId).getCurrentSimplified()

        if (word == "") return false

        Utils.playWordInBackground(context, word)

        return true
    }

    fun openDictionary() {
        val word = getWidgetPreferences(context, widgetId).getCurrentSimplified()

        getOpenDictionaryIntent(word).send()
    }

    fun getOpenDictionaryIntent(word : String) : PendingIntent {
        return PendingIntent.getActivity(
            context,
            0,
            Intent(
                Intent.ACTION_VIEW, Uri.parse("https://www.wordsense.eu/$word/")),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        private var instances = mutableMapOf<Int, FlashcardManager>()

        fun getInstance(context: Context, widgetId: Int) : FlashcardManager {
            if (instances[widgetId] == null)
                instances[widgetId] = FlashcardManager(context, widgetId)

            return instances[widgetId]!!
        }
    }
}