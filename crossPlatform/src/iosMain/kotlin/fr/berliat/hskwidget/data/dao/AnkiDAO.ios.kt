package fr.berliat.hskwidget.data.dao

actual class AnkiNoteInfo {
    actual val id: Long
        get() = TODO("Not yet implemented")
    actual val fields: Array<String?>?
        get() = TODO("Not yet implemented")
    actual val tags: MutableSet<String?>
        get() = TODO("Not yet implemented")
    actual val key: String?
        get() = TODO("Not yet implemented")
}

actual class AnkiDAO {
    actual suspend fun deleteNote(noteId: Long): Boolean {
        TODO("Not yet implemented")
    }

    actual suspend fun updateNoteDeck(noteId: Long, newDeckId: Long): Int {
        TODO("Not yet implemented")
    }

    actual suspend fun getDeckList(): Map<Long, String>? {
        return null
    }

    actual suspend fun addNewDeck(name: String): Long? {
        TODO("Not yet implemented")
    }

    actual suspend fun getModelName(mid: Long): String? {
        TODO("Not yet implemented")
    }

    actual suspend fun isModelExist(mid: Long): Boolean {
        TODO("Not yet implemented")
    }

    actual suspend fun addNewCustomModel(
        name: String,
        fields: Array<String>,
        cards: Array<String>,
        qfmt: Array<String>,
        afmt: Array<String>,
        css: String?,
        did: Long?,
        sortf: Int?
    ): Long? {
        TODO("Not yet implemented")
    }

    actual suspend fun getNote(noteId: Long): AnkiNoteInfo? {
        TODO("Not yet implemented")
    }

    actual suspend fun addNote(
        modelId: Long,
        deckId: Long,
        fields: Array<String>,
        tags: Set<String>?
    ): Long? {
        TODO("Not yet implemented")
    }

    actual suspend fun updateNoteTags(
        noteId: Long,
        tags: Set<String>
    ): Boolean {
        TODO("Not yet implemented")
    }

    actual suspend fun updateNoteFields(
        noteId: Long,
        fields: Array<String>
    ): Boolean {
        TODO("Not yet implemented")
    }
}