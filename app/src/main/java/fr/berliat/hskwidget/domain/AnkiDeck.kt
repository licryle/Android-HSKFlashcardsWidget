package fr.berliat.hskwidget.domain

import android.content.Context
import fr.berliat.hskwidget.R
import fr.berliat.hskwidget.data.model.WordList
import fr.berliat.hskwidget.data.store.AnkiStore
import fr.berliat.hskwidget.data.store.ChineseWordsDatabase

class AnkiDeck private constructor(private val context: Context,
                                   private val wordList: WordList
) {
    private var _ankiId : Long = wordList.ankiDeckId

    val ankiId: Long
        get() = _ankiId

    internal suspend fun setAnkiId(newAnkiId: Long) {
        _ankiId = newAnkiId

        ChineseWordsDatabase.getInstance(context).wordListDAO()
            .updateAnkiDeckId(wordList.id, newAnkiId)
    }

    fun getAnkiDeckName(): String {
        return getDeckNamePrefix() + wordList.name
    }

    fun getDeckNamePrefix() : String {
        return context.getString(R.string.app_name) + ": "
    }

    companion object {
        suspend fun getOrCreate(
            context: Context,
            store: AnkiStore,
            wordList: WordList
        ): AnkiDeck {
            val deck = AnkiDeck(context, wordList)
            val decks = store.api.getDeckList()?: emptyMap()

            if (deck.ankiId == WordList.ANKI_ID_EMPTY || decks.none { it.key == deck.ankiId }) {
                val ankiDeckId : Long
                // So deckId is non-existent or a goner. Let's piggy back by name given we have a prefix
                val sameNameDeck = decks.filterValues { it == deck.getAnkiDeckName() }
                if (sameNameDeck.isNotEmpty()) {
                    ankiDeckId = sameNameDeck.keys.first()
                } else {
                    ankiDeckId = store.api.addNewDeck(deck.getAnkiDeckName())
                        ?: throw IllegalStateException("Couldn't fetch or create Deck in Anki.")
                }

                deck.setAnkiId(ankiDeckId)
            }

            return deck
        }
    }
}