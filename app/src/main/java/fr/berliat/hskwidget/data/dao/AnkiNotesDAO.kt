package fr.berliat.hskwidget.data.dao

import android.content.Context
import com.ichi2.anki.FlashCardsContract.Note
import com.ichi2.anki.api.AddContentApi
import com.ichi2.anki.api.NoteInfo

/**
 * To check how to use the contentResolver, check out AnkiDroid's code at:
 * https://github.com/ankidroid/Anki-Android/blob/main/api/src/main/java/com/ichi2/anki/api/AddContentApi.kt
 */
class AnkiNotesDAO(context: Context) {
    private val api = AddContentApi(context)
    private val resolver = context.contentResolver

    /**
     * Deletes a note from AnkiDroid given a note ID.
     * Returns true if the note was deleted successfully.
     */
    fun deleteNote(noteId: Long): Boolean {
        return try {
            val builder = Note.CONTENT_URI.buildUpon()
            val contentUri = builder.appendPath(noteId.toString()).build()
            val rowsDeleted = resolver.delete(contentUri, null, null)
            rowsDeleted > 0
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun getDeckList(): Map<Long, String>? = api.getDeckList()
    fun addNewDeck(name: String): Long? = api.addNewDeck(name)
    fun getDeckName(deckId: Long): String? = api.getDeckName(deckId)

    fun getModelList(minNumFields: Int): Map<Long, String>? = api.getModelList(minNumFields)
    fun getModelName(mid: Long): String? = api.getModelName(mid)
    fun addNewCustomModel(
        name: String,
        fields: Array<String>,
        cards: Array<String>,
        qfmt: Array<String>,
        afmt: Array<String>,
        css: String?,
        did: Long?,
        sortf: Int?) : Long? {
        return api.addNewCustomModel(name, fields, cards, qfmt, afmt, css, did, sortf)
    }

    fun getNote(noteId: Long): NoteInfo? = api.getNote(noteId)
    fun addNote(modelId: Long, deckId: Long, fields: Array<String>, tags: Set<String>?): Long? {
        return api.addNote(modelId, deckId, fields, tags)
    }
    fun updateNoteTags(noteId: Long, tags: Set<String>): Boolean = api.updateNoteTags(noteId, tags)
    fun updateNoteFields(noteId: Long, fields: Array<String>): Boolean = api.updateNoteFields(noteId, fields)
    fun getFieldList(modelId: Long): Array<String>? = api.getFieldList(modelId)
}
