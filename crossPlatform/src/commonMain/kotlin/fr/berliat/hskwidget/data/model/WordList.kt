package fr.berliat.hskwidget.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

import hskflashcardswidget.crossplatform.generated.resources.Res
import hskflashcardswidget.crossplatform.generated.resources.app_name

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

import org.jetbrains.compose.resources.getString

@Entity(tableName = "word_list")
data class WordList constructor(
    val name: String,
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "creation_date") val creationDate: Instant = Clock.System.now(),
    @ColumnInfo(name = "last_modified") val lastModified: Instant = Clock.System.now(), // Add this field with current timestamp as default
    @ColumnInfo(name = "anki_deck_id") val ankiDeckId: Long = 0,
    @ColumnInfo(name = "list_type") val listType: ListType = ListType.USER
) {
    suspend fun getAnkiDeckName(): String {
        return getDeckNamePrefix() + name
    }

    suspend fun getDeckNamePrefix() : String {
        return getString(Res.string.app_name) + ": "
    }

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
    tableName = "word_list_entry",
    primaryKeys = ["list_id", "simplified", "anki_note_id"],
    foreignKeys = [
        ForeignKey(
            entity = WordList::class,
            parentColumns = ["id"],
            childColumns = ["list_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("list_id"), Index("simplified")]
)

data class WordListEntry(
    @ColumnInfo(name = "list_id") val listId: Long,
    val simplified: String,
    @ColumnInfo(name = "anki_note_id") val ankiNoteId: Long = 0
)

data class WordListWithCount(
    val name: String,
    val id: Long,
    val creationDate: Instant,
    val lastModified: Instant,
    val ankiDeckId: Long,
    val listType: WordList.ListType,
    val wordCount: Int
) {
    val wordList: WordList
        get () {
            return WordList(name, id, creationDate, lastModified, ankiDeckId, listType)
        }
}