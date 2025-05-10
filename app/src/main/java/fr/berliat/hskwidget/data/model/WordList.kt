package fr.berliat.hskwidget.data.model

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Junction
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "word_lists")
data class WordList(
    val name: String,
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val creationDate: Long = System.currentTimeMillis(),
    val lastModified: Long = System.currentTimeMillis() // Add this field with current timestamp as default
)

@Entity(
    tableName = "word_list_entries",
    primaryKeys = ["listId", "wordId"],
    foreignKeys = [
        ForeignKey(
            entity = WordList::class,
            parentColumns = ["id"],
            childColumns = ["listId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ChineseWord::class,
            parentColumns = ["simplified"],
            childColumns = ["wordId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("listId"), Index("wordId")]
)

data class WordListEntry(
    val listId: Long,
    val wordId: String
)

data class WordListWithWords(
    @Embedded val wordList: WordList,
    @Relation(
        parentColumn = "id",
        entityColumn = "simplified",
        associateBy = Junction(
            value = WordListEntry::class,
            parentColumn = "listId",
            entityColumn = "wordId"
        )
    )
    val words: List<ChineseWord>
) 