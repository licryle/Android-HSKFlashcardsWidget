package fr.berliat.hskwidget.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

import hskflashcardswidget.crossplatform.generated.resources.Res
import hskflashcardswidget.crossplatform.generated.resources.app_name
import org.jetbrains.compose.resources.getString

import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@Entity(tableName = "word_list")
data class WordList @OptIn(ExperimentalTime::class) constructor(
    val name: String,
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "creation_date") val creationDate: Long = Clock.System.now().toEpochMilliseconds(),
    @ColumnInfo(name = "last_modified") val lastModified: Long = Clock.System.now().toEpochMilliseconds(), // Add this field with current timestamp as default
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
    @ColumnInfo(name = "creation_date") val creationDate: Long,
    @ColumnInfo(name = "last_modified") val lastModified: Long,
    @ColumnInfo(name = "anki_deck_id") val ankiDeckId: Long,
    @ColumnInfo(name = "list_type") val listType: WordList.ListType,
    @ColumnInfo(name = "word_count") val wordCount: Int
) {
    val wordList: WordList
        get () {
            return WordList(name, id, creationDate, lastModified, ankiDeckId, listType)
        }
}