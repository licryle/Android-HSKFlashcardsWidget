package fr.berliat.hskwidget.domain

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.util.Log
import fr.berliat.hskwidget.MainActivity
import fr.berliat.hskwidget.data.model.ChineseWord
import fr.berliat.hskwidget.data.store.ChineseWordsDatabase
import fr.berliat.hskwidget.data.store.FlashcardPreferencesStore
import fr.berliat.hskwidget.ui.widget.FlashcardFragment
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

    private suspend fun AnnotatedChineseWordDAO() = ChineseWordsDatabase.getInstance(context).annotatedChineseWordDAO()

    fun getPreferenceStore(): FlashcardPreferencesStore {
        return FlashcardPreferencesStore(context, widgetId)
    }

    suspend fun getCurrentWord() : ChineseWord? {
        if (flashCardPrefs.currentSimplified == null) return null

        val currentWord = AnnotatedChineseWordDAO().getFromSimplified(flashCardPrefs.currentSimplified)
            ?: return null

        return currentWord.toChineseWord()
    }

    suspend fun getNewWord(): ChineseWord? {
        val currentWord = AnnotatedChineseWordDAO().getFromSimplified(flashCardPrefs.currentSimplified)

        val allowedListIds = flashCardPrefs.getAllowedLists().map { it.wordList.id }
        val newWord = AnnotatedChineseWordDAO().getRandomWordFromLists(allowedListIds, arrayOf(currentWord?.simplified ?: ""))

        if (newWord == null) {
            Log.i(TAG, "getNewWord: No new word gotten, null returned.")
            flashCardPrefs.currentSimplified = null
            return null
        }

        Log.i(TAG, "getNewWord: Got a new word, maybe: %s".format(newWord))

        // Persist it in preferences for cross-App convenience
        flashCardPrefs.setCurrentSimplified(newWord.simplified, null).await()

        return newWord.toChineseWord()
    }

    fun updateWord() {
        Log.i(TAG, "Word update requested")
        coroutineScope.launch(Dispatchers.IO) {
            getNewWord()

            // Switch back to the main thread to update UI
            withContext(Dispatchers.Main) {
                Log.i(TAG, "Now calling for fragments' update")
                if (fragments[widgetId] != null)
                    fragments[widgetId]!!.forEach { it.updateFlashcardView() }
            }

            Log.i(TAG, "Now calling for widgets' update")
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
        val word = flashCardPrefs.currentSimplified ?: return false

        Utils.playWordInBackground(context, word)

        return true
    }

    fun openDictionary() {
        val query = SearchQuery.fromString(flashCardPrefs.currentSimplified)
        query.ignoreAnnotation = true

        val confIntent = Intent(context, MainActivity::class.java)
        confIntent.putExtra(MainActivity.INTENT_SEARCH_WORD, query.toString())
        confIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP

        Utils.logAnalyticsEvent(Utils.ANALYTICS_EVENTS.WIDGET_OPEN_DICTIONARY)

        context.startActivity(confIntent)
    }

    companion object {
        const val TAG = "FlashcardManager"
        private var instances = mutableMapOf<Int, FlashcardManager>()

        fun getInstance(context: Context, widgetId: Int) =
            instances[widgetId] ?: synchronized(this) {
                instances[widgetId] ?: FlashcardManager(context, widgetId)
                    .also { instances[widgetId] = it }
            }
    }
}