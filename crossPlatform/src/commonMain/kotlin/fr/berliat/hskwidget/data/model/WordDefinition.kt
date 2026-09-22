package fr.berliat.hskwidget.data.model

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index

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
