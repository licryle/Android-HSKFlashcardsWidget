package fr.berliat.hskwidget.domain

import android.content.Context
import android.util.Log
import com.ichi2.anki.api.NoteInfo
import fr.berliat.hskwidget.R
import fr.berliat.hskwidget.data.dao.AnnotatedChineseWord
import fr.berliat.hskwidget.data.model.WordList
import fr.berliat.hskwidget.data.model.WordListEntry
import fr.berliat.hskwidget.data.store.AnkiIntegrationStore
import fr.berliat.hskwidget.data.store.ChineseWordsDatabase
import java.util.Locale


class AnkiDroidHelper(context: Context) {
    private val ankiStore = AnkiIntegrationStore(context)
    private val database = ChineseWordsDatabase.getInstance(context)

    val store: AnkiIntegrationStore
        get() = ankiStore

    class AnkiDeck private constructor(private val context: Context,
                                       private val wordList: WordList) {
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
            const val DECK_LOCALID_GLOBAL = 100 // Totally arbitrary

            suspend fun getOrCreate(
                context: Context,
                store: AnkiIntegrationStore,
                wordList: WordList
            ): AnkiDeck {
                val deck = AnkiDeck(context, wordList)
                val decks = store.api.getDeckList()?: emptyMap()

                if (deck.ankiId == WordList.ANKI_ID_EMPTY || decks.none { it.key == deck.ankiId }) {
                    var ankiDeckId = 0L
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

    suspend fun isAnkiRunning() : Boolean {
        return store.isStoreReady()
    }

    suspend fun importOrUpdateCard(deck: AnkiDeck, wordEntry: WordListEntry, word: AnnotatedChineseWord): Long? {
        Log.d(AnkiIntegrationStore.TAG, "importOrUpdateCard: ${word.simplified} to Anki")
        val modelId = ankiStore.getOrCreateModelId() ?: return null
        if (deck.ankiId == WordList.ANKI_ID_EMPTY) throw IllegalStateException("Couldn't create a new Deck in Anki")

        with (word.annotation) {
            val fields = arrayOf(
                word.simplified,
                word.word?.pinyins?.toString() ?: "",
                word.word?.definition?.get(Locale.ENGLISH) ?: "",
                this?.notes ?: "",
                this?.firstSeen?.toString() ?: "",
                word.word?.hskLevel?.toString() ?: "",
                this?.level?.toString() ?: "",
                this?.classType?.toString() ?: "",
                this?.themes ?: ""
            )

            val tags: MutableSet<String> = mutableSetOf(
                word.word?.hskLevel?.toString() ?: "",
                this?.level?.toString() ?: "",
                this?.classType?.toString() ?: ""
            ).apply {
                addAll(this@with?.themes?.split(",") ?: emptyList())
            }


            var note : NoteInfo? = null
            if (wordEntry.ankiNoteId != WordList.ANKI_ID_EMPTY) {
                note = ankiStore.api.getNote(wordEntry.ankiNoteId)
            }

            if (note != null && note.fields.size > 1 && note.fields[0] == word.simplified.trim().replace("\r\n","\n")) {
                Log.d(TAG, "importOrUpdateCard: calling api.updates")
                ankiStore.api.updateNoteFields(note.id, fields)
                ankiStore.api.updateNoteTags(note.id, tags)
                ankiStore.api.updateNoteDeck(note.id, deck.ankiId)
                return note.id
            } else {
                val ankiNoteId = ankiStore.api.addNote(modelId, deck.ankiId, fields, tags)

                if (ankiNoteId != null) {
                    database.wordListDAO().updateAnkiNoteId(wordEntry.listId, wordEntry.simplified, ankiNoteId)
                }

                return ankiNoteId
            }
        }
    }

    enum class ACTION {
        UPDATE,
        DELETE
    }

    companion object {
        const val TAG = "AnkiDroidHelper"
    }
}
