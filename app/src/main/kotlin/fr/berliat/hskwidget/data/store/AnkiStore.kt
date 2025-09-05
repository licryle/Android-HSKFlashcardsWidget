package fr.berliat.hskwidget.data.store


import android.content.Context
import android.util.Log
import com.ichi2.anki.api.NoteInfo
import fr.berliat.hskwidget.R
import fr.berliat.hskwidget.data.dao.AnkiDAO
import fr.berliat.hskwidget.data.model.AnnotatedChineseWord
import fr.berliat.hskwidget.data.model.WordList
import fr.berliat.hskwidget.data.model.WordListEntry
import fr.berliat.hskwidget.domain.AnkiDeck
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

class AnkiStore(val context: Context):
    PrefixedPreferenceDataStoreBridge(context.dataStore, "anki") {
    val api: AnkiDAO = AnkiDAO(context)

    private suspend fun database() = withContext(Dispatchers.IO) { DatabaseHelper.getInstance(context) }

    suspend fun isStoreReady() : Boolean = withContext(Dispatchers.IO) {
        api.getDeckList() != null
    }

    suspend fun getOrCreateModelId(): Long? = withContext(Dispatchers.IO) {
        var mid : Long? = getModelId()

        if (mid == null || mid.toInt() == 0 || !api.isModelExist(mid)) {
            // Not found, let's add
            mid = createModel()

            if (mid == null) {
                Log.i(TAG, "findOrCreateModelIdByName: Couldn't add model to Anki")
                return@withContext null
            }

            Log.i(TAG, "findOrCreateModelIdByName: Inserted new Model into Anki")
            setModelId(mid)
        }

        return@withContext mid
    }

    suspend fun deleteCard(word: WordListEntry): Boolean = withContext(Dispatchers.IO) {
        word.ankiNoteId.let { api.deleteNote(it) }
    }

    private fun setModelId(modelId: Long) {
        putLong("model", modelId)
    }

    private fun getModelId(): Long {
        return getLong("model", -1)
    }

    private fun getModelName() : String { return context.getString(R.string.app_name) }

    private suspend fun createModel() : Long? = withContext(Dispatchers.IO) {
        api.addNewCustomModel(
            getModelName(),
            FIELDS,
            CARD_NAMES,
            arrayOf(
                context.getString(R.string.anki_card_ENCN_front),
                context.getString(R.string.anki_card_CNEN_front)
            ),
            arrayOf(
                context.getString(R.string.anki_card_ENCN_back),
                context.getString(R.string.anki_card_CNEN_back)
            ),
            context.getString(R.string.anki_card_css),
            null,
            null
        )
    }

    suspend fun importOrUpdateCard(deck: AnkiDeck, wordEntry: WordListEntry, word: AnnotatedChineseWord): Long?
            = withContext(Dispatchers.IO) {
        Log.d(TAG, "importOrUpdateCard: ${word.simplified} to Anki")
        val modelId = getOrCreateModelId() ?: return@withContext null
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
                note = api.getNote(wordEntry.ankiNoteId)
            }

            if (note != null && note.fields.size > 1 && note.fields[0] == word.simplified.trim().replace("\r\n","\n")) {
                Log.d(TAG, "importOrUpdateCard: calling api.updates")
                api.updateNoteFields(note.id, fields)
                api.updateNoteTags(note.id, tags)
                api.updateNoteDeck(note.id, deck.ankiId)
                return@withContext note.id
            } else {
                val ankiNoteId = api.addNote(modelId, deck.ankiId, fields, tags)

                if (ankiNoteId != null) {
                    database().wordListDAO().updateAnkiNoteId(wordEntry.listId, wordEntry.simplified, ankiNoteId)
                }

                return@withContext ankiNoteId
            }
        }
    }

    companion object {
        const val TAG = "AnkiDroidHelper"

        // List of field names that will be used in AnkiDroid model
        val FIELDS: Array<String> = arrayOf(
            "Hanzi", "Pinyin", "Definition", "Notes", "FirstSeen", "HSK", "Level", "Class", "Themes"
        )

        // List of card names that will be used in AnkiDroid (one for each direction of learning)
        val CARD_NAMES: Array<String> = arrayOf("English>Chinese", "Chinese>English")
    }
}