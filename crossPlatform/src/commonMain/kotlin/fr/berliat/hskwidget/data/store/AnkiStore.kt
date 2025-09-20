package fr.berliat.hskwidget.data.store

import co.touchlab.kermit.Logger
import fr.berliat.hskwidget.core.Locale
import fr.berliat.hskwidget.data.dao.AnkiDAO
import fr.berliat.hskwidget.data.dao.AnkiNoteInfo
import fr.berliat.hskwidget.data.dao.WordListDAO
import fr.berliat.hskwidget.data.model.AnnotatedChineseWord
import fr.berliat.hskwidget.data.model.WordList
import fr.berliat.hskwidget.data.model.WordListEntry
import hskflashcardswidget.crossplatform.generated.resources.Res
import hskflashcardswidget.crossplatform.generated.resources.anki_card_CNEN_back
import hskflashcardswidget.crossplatform.generated.resources.anki_card_CNEN_front
import hskflashcardswidget.crossplatform.generated.resources.anki_card_ENCN_back
import hskflashcardswidget.crossplatform.generated.resources.anki_card_ENCN_front
import hskflashcardswidget.crossplatform.generated.resources.anki_card_css
import hskflashcardswidget.crossplatform.generated.resources.app_name
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.getString

class AnkiStore(
    val ankiDAO : AnkiDAO,
    val wordListDAO : WordListDAO,
    val appPreferences : AppPreferencesStore
) {
    companion object {
        const val TAG = "AnkiDroidHelper"

        // List of field names that will be used in AnkiDroid model
        val FIELDS: Array<String> = arrayOf(
            "Hanzi", "Pinyin", "Definition", "Notes", "FirstSeen", "HSK", "Level", "Class", "Themes"
        )

        // List of card names that will be used in AnkiDroid (one for each direction of learning)
        val CARD_NAMES: Array<String> = arrayOf("English>Chinese", "Chinese>English")
    }
    suspend fun isStoreReady() : Boolean = withContext(Dispatchers.IO) {
        ankiDAO.getDeckList() != null
    }

    suspend fun getOrCreateModelId(): Long? = withContext(Dispatchers.IO) {
        var mid : Long? = getModelId()

        if (mid == null || mid.toInt() == 0 || !ankiDAO.isModelExist(mid)) {
            // Not found, let's add
            mid = createModel()

            if (mid == null) {
                Logger.i(tag = TAG, messageString = "findOrCreateModelIdByName: Couldn't add model to Anki")
                return@withContext null
            }

            Logger.i(tag = TAG, messageString = "findOrCreateModelIdByName: Inserted new Model into Anki")
            setModelId(mid)
        }

        return@withContext mid
    }

    suspend fun deleteCard(word: WordListEntry): Boolean = withContext(Dispatchers.IO) {
        word.ankiNoteId.let { ankiDAO.deleteNote(it) }
    }

    private fun setModelId(modelId: Long) {
        appPreferences.ankiModelId.value = modelId
    }

    private fun getModelId(): Long {
        return appPreferences.ankiModelId.value
    }

    private suspend fun getModelName() : String { return getString(Res.string.app_name) }

    private suspend fun createModel() : Long? = withContext(Dispatchers.IO) {
        ankiDAO.addNewCustomModel(
            getModelName(),
            FIELDS,
            CARD_NAMES,
            arrayOf(
                getString(Res.string.anki_card_ENCN_front),
                getString(Res.string.anki_card_CNEN_front)
            ),
            arrayOf(
                getString(Res.string.anki_card_ENCN_back),
                getString(Res.string.anki_card_CNEN_back)
            ),
            getString(Res.string.anki_card_css),
            null,
            null
        )
    }

    suspend fun importOrUpdateCard(deck: WordList, wordEntry: WordListEntry, word: AnnotatedChineseWord): Long?
            = withContext(Dispatchers.IO) {
        Logger.d(tag = TAG, messageString = "importOrUpdateCard: ${word.simplified} to Anki")
        val modelId = getOrCreateModelId() ?: return@withContext null
        if (deck.ankiDeckId == WordList.Companion.ANKI_ID_EMPTY) throw IllegalStateException("Couldn't create a new Deck in Anki")

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


            var note : AnkiNoteInfo? = null
            if (wordEntry.ankiNoteId != WordList.Companion.ANKI_ID_EMPTY) {
                note = ankiDAO.getNote(wordEntry.ankiNoteId)
            }

            if (note != null && note.fields != null && note.fields!!.size > 1
                && note.fields!![0] == word.simplified.trim().replace("\r\n","\n")) {
                Logger.d(tag = TAG, messageString = "importOrUpdateCard: calling api.updates")
                ankiDAO.updateNoteFields(note.id, fields)
                ankiDAO.updateNoteTags(note.id, tags)
                ankiDAO.updateNoteDeck(note.id, deck.ankiDeckId)
                return@withContext note.id
            } else {
                val ankiNoteId = ankiDAO.addNote(modelId, deck.ankiDeckId, fields, tags)

                if (ankiNoteId != null) {
                    wordListDAO.updateAnkiNoteId(wordEntry.listId, wordEntry.simplified, ankiNoteId)
                }

                return@withContext ankiNoteId
            }
        }
    }
}