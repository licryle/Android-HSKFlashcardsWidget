package fr.berliat.hskwidget.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/** A dictionary definition in one explicitly identified language. */
@Entity(
    tableName = "word_definition",
    primaryKeys = ["simplified", "language"],
    foreignKeys = [ForeignKey(
        entity = ChineseWord::class,
        parentColumns = ["simplified"],
        childColumns = ["simplified"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["language", "definition"])]
)
data class WordDefinition(
    val simplified: String,
    val language: String,
    val definition: String
)
