package fr.berliat.hskwidget.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import fr.berliat.hskwidget.data.model.WordList.ListType
import java.time.Instant

@Entity(tableName = "word_lists")
data class WordList(
    val name: String,
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val creationDate: Long = Instant.now().toEpochMilli(),
    val lastModified: Long = Instant.now().toEpochMilli(), // Add this field with current timestamp as default
    val ankiDeckId: Long = 0,
    val listType: ListType = ListType.USER
) {

    enum class ListType (val type: String) {
        USER("USER"),
        SYSTEM("SYSTEM");

        companion object {
            infix fun from(findValue: String): ListType = ListType.valueOf(findValue)
        }
    }

    companion object {
        const val ANKI_ID_EMPTY: Long = 0
        const val SYSTEM_ANNOTATED_NAME = "Annotated Words"
    }
}

@Entity(
    tableName = "word_list_entries",
    primaryKeys = ["listId", "simplified", "ankiNoteId"],
    foreignKeys = [
        ForeignKey(
            entity = WordList::class,
            parentColumns = ["id"],
            childColumns = ["listId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("listId"), Index("simplified")]
)

data class WordListEntry(
    val listId: Long,
    val simplified: String,
    val ankiNoteId: Long = 0
)

data class WordListWithCount(
    val name: String,
    val id: Long,
    val creationDate: Long,
    val lastModified: Long,
    val ankiDeckId: Long,
    val listType: ListType,
    val wordCount: Int
) {
    val wordList: WordList
        get () {
            return WordList(name, id, creationDate, lastModified, ankiDeckId, listType)
        }
}