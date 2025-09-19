// commonMain
package fr.berliat.hskwidget.data.dao

/**
 * Cross-platform contract for interacting with Anki.
 * Android actual talks to AnkiDroid via ContentResolver.
 * iOS actual may be unimplemented or bridged via another API.
 */
expect class AnkiNoteInfo {
    val id: Long
    val fields: Array<String?>?
    val tags: MutableSet<String?>
    val key: String?
}

expect class AnkiDAO {
    suspend fun deleteNote(noteId: Long): Boolean
    suspend fun updateNoteDeck(noteId: Long, newDeckId: Long): Int

    suspend fun getDeckList(): Map<Long, String>?
    suspend fun addNewDeck(name: String): Long?
    suspend fun getModelName(mid: Long): String?
    suspend fun isModelExist(mid: Long): Boolean

    suspend fun addNewCustomModel(
        name: String,
        fields: Array<String>,
        cards: Array<String>,
        qfmt: Array<String>,
        afmt: Array<String>,
        css: String?,
        did: Long?,
        sortf: Int?
    ): Long?

    suspend fun getNote(noteId: Long): AnkiNoteInfo?
    suspend fun addNote(modelId: Long, deckId: Long, fields: Array<String>, tags: Set<String>?): Long?
    suspend fun updateNoteTags(noteId: Long, tags: Set<String>): Boolean
    suspend fun updateNoteFields(noteId: Long, fields: Array<String>): Boolean
}
