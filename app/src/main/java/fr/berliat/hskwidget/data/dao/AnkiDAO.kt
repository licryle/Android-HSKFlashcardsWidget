package fr.berliat.hskwidget.data.dao

import android.content.ContentValues
import android.content.Context
import com.ichi2.anki.FlashCardsContract
import com.ichi2.anki.FlashCardsContract.Note
import com.ichi2.anki.api.AddContentApi
import com.ichi2.anki.api.NoteInfo

/**
 * To check how to use the contentResolver, check out AnkiDroid's code at:
 * https://github.com/ankidroid/Anki-Android/blob/main/api/src/main/java/com/ichi2/anki/api/AddContentApi.kt
 * https://github.com/mmjang/ankihelper/blob/master/app/src/main/java/com/ichi2/anki/FlashCardsContract.java
 * And in particular (for the API itself):
 * https://github.com/ankidroid/Anki-Android/blob/main/AnkiDroid/src/main/java/com/ichi2/anki/provider/CardContentProvider.kt
 */
class AnkiDAO(context: Context) {
    private val api = AddContentApi(context)
    private val resolver = context.contentResolver

    /**
     * Deletes a note from AnkiDroid given a note ID.
     * Returns true if the note was deleted successfully.
     */
    suspend fun deleteNote(noteId: Long): Boolean {
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

    suspend fun updateNoteDeck(noteId: Long, newDeckId: Long): Int {
        val values = ContentValues().apply {
            put(FlashCardsContract.Card.DECK_ID, newDeckId) // newDeckId is a Long
        }

        var toUpdate = true
        var i = 0
        while (toUpdate) {
            try {
                val builder = Note.CONTENT_URI.buildUpon()
                val contentUri = builder.appendPath(noteId.toString()).appendPath("cards").appendPath(i.toString()).build()
                val rowsUpdated = resolver.update(contentUri, values, null, null)
                toUpdate = rowsUpdated > 0
                i += 1
            } catch (e: Exception) {
                toUpdate = false
                e.printStackTrace()
            }
        }

        return i
    }

    /*fun getNotesFromDeck(deckId: Long): List<Long> {
        val notes = mutableListOf<Long>()

        val builder = Note.CONTENT_URI_V2.buildUpon()
        val contentUri = builder.appendPath(deckId.toString()).build()

        val projection = arrayOf(Note._ID)
        val query = "DID:\"$modelName\""
        resolver.query(contentUri, projection, null, null, null)
    }*/

    suspend fun getDeckList(): Map<Long, String>? = api.getDeckList()
    suspend fun addNewDeck(name: String): Long? = api.addNewDeck(name)
    suspend fun getModelName(mid: Long): String? = api.getModelName(mid)
    suspend fun isModelExist(mid: Long): Boolean = getModelName(mid) != null

    suspend fun addNewCustomModel(
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

    suspend fun getNote(noteId: Long): NoteInfo? = api.getNote(noteId)
    suspend fun addNote(modelId: Long, deckId: Long, fields: Array<String>, tags: Set<String>?): Long? {
        return api.addNote(modelId, deckId, fields, tags)
    }
    suspend fun updateNoteTags(noteId: Long, tags: Set<String>): Boolean = api.updateNoteTags(noteId, tags)
    suspend fun updateNoteFields(noteId: Long, fields: Array<String>): Boolean = api.updateNoteFields(noteId, fields)
}
