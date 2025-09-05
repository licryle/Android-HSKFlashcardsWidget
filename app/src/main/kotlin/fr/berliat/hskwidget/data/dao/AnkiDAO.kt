package fr.berliat.hskwidget.data.dao

import android.content.ContentValues
import android.content.Context
import android.util.Log
import com.ichi2.anki.FlashCardsContract
import com.ichi2.anki.FlashCardsContract.Note
import com.ichi2.anki.api.AddContentApi
import com.ichi2.anki.api.NoteInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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

    companion object {
        private const val TAG = "AnkiDAO"
    }

    /**
     * Deletes a note from AnkiDroid given a note ID.
     * Returns true if the note was deleted successfully.
     */
    suspend fun deleteNote(noteId: Long): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val builder = Note.CONTENT_URI.buildUpon()
            val contentUri = builder.appendPath(noteId.toString()).build()
            val rowsDeleted = resolver.delete(contentUri, null, null)
            rowsDeleted > 0
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun updateNoteDeck(noteId: Long, newDeckId: Long): Int = withContext(Dispatchers.IO) {
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
                Log.d(TAG, "Updated $i cards for note $noteId")
            }
        }

        return@withContext i
    }

    /*fun getNotesFromDeck(deckId: Long): List<Long> {
        val notes = mutableListOf<Long>()

        val builder = Note.CONTENT_URI_V2.buildUpon()
        val contentUri = builder.appendPath(deckId.toString()).build()

        val projection = arrayOf(Note._ID)
        val query = "DID:\"$modelName\""
        resolver.query(contentUri, projection, null, null, null)
    }*/

    suspend fun getDeckList(): Map<Long, String>? = withContext(Dispatchers.IO) { api.getDeckList() }
    suspend fun addNewDeck(name: String): Long? = withContext(Dispatchers.IO) { api.addNewDeck(name) }
    suspend fun getModelName(mid: Long): String? = withContext(Dispatchers.IO) { api.getModelName(mid) }
    suspend fun isModelExist(mid: Long): Boolean = withContext(Dispatchers.IO) { getModelName(mid) != null }

    suspend fun addNewCustomModel(
        name: String,
        fields: Array<String>,
        cards: Array<String>,
        qfmt: Array<String>,
        afmt: Array<String>,
        css: String?,
        did: Long?,
        sortf: Int?) : Long? = withContext(Dispatchers.IO) {
        api.addNewCustomModel(name, fields, cards, qfmt, afmt, css, did, sortf)
    }

    suspend fun getNote(noteId: Long): NoteInfo? = withContext(Dispatchers.IO) { api.getNote(noteId) }
    suspend fun addNote(modelId: Long, deckId: Long, fields: Array<String>, tags: Set<String>?): Long?
        = withContext(Dispatchers.IO) { api.addNote(modelId, deckId, fields, tags) }
    suspend fun updateNoteTags(noteId: Long, tags: Set<String>): Boolean
        = withContext(Dispatchers.IO) { api.updateNoteTags(noteId, tags) }
    suspend fun updateNoteFields(noteId: Long, fields: Array<String>): Boolean
        = withContext(Dispatchers.IO) { api.updateNoteFields(noteId, fields) }
}
