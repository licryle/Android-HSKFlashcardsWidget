package fr.berliat.hskwidget.domain

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.util.Log
import fr.berliat.hskwidget.MainActivity
import fr.berliat.hskwidget.data.model.ChineseWord
import fr.berliat.hskwidget.data.store.ChineseWordsDatabase
import fr.berliat.hskwidget.data.store.FlashcardPreferencesStore
import fr.berliat.hskwidget.ui.flashcard.FlashcardFragment
import fr.berliat.hskwidget.ui.widget.FlashcardWidgetProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FlashcardManager private constructor(private val context: Context,
                                           private val widgetId: Int) {
    private val fragments = mutableMapOf<Int, MutableSet<FlashcardFragment>>()
    private val flashCardPrefs = getPreferenceStore()
    private val appWidgetMgr = AppWidgetManager.getInstance(context)
    private val coroutineScope = Utils.getAppScope(context)


    private suspend fun ChineseWordDAO() = ChineseWordsDatabase.getInstance(context).chineseWordDAO()

    fun getPreferenceStore(): FlashcardPreferencesStore {
        return FlashcardPreferencesStore(context, widgetId)
    }

    suspend fun getCurrentWord() : ChineseWord {
        var currentWord = ChineseWordDAO().findWordFromSimplified(flashCardPrefs.currentSimplified)

        if (currentWord == null) currentWord = Utils.getDefaultWord(context)

        return currentWord
    }

    suspend fun getNewWord(): ChineseWord {
        val currentWord = ChineseWordDAO().findWordFromSimplified(flashCardPrefs.currentSimplified)

        var newWord = ChineseWordDAO().getRandomHSKWord(flashCardPrefs.getAllowedHSK(), setOf(currentWord!!))

        if (newWord == null) newWord = Utils.getDefaultWord(context)

        // Persist it in preferences for cross-App convenience
        flashCardPrefs.currentSimplified = newWord.simplified

        return newWord
    }

    fun updateWord() {
        Log.i("FlashcardManager", "Word update requested")
        coroutineScope.launch(Dispatchers.IO) {
            getNewWord()

            // Switch back to the main thread to update UI
            withContext(Dispatchers.Main) {
                Log.i("FlashcardManager", "Now calling for fragments' update")
                if (fragments[widgetId] != null)
                    fragments[widgetId]!!.forEach { it.updateFlashcardView() }
            }

            Log.i("FlashcardManager", "Now calling for widgets' update")
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
        val word = flashCardPrefs.currentSimplified

        if (word == "") return false

        Utils.playWordInBackground(context, word)

        return true
    }

    fun openDictionary() {
        val word = flashCardPrefs.currentSimplified

        val confIntent = Intent(context, MainActivity::class.java)
        confIntent.putExtra(MainActivity.INTENT_SEARCH_WORD, word)
        confIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP

        context.startActivity(confIntent)
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