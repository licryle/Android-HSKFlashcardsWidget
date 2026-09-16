package fr.berliat.hskwidget.data.model

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.Fts5
import androidx.room3.FtsOptions

@Fts5(contentEntity = ChineseWord::class, tokenizer = FtsOptions.TOKENIZER_UNICODE61)
@Entity(tableName = "chinese_word_fts")
data class ChineseWordFTS(
    @ColumnInfo(name = "searchable_text") val searchableText: String,
    val simplified: String,
    val traditional: String?,
)

@Fts5(contentEntity = WordDefinition::class, tokenizer = FtsOptions.TOKENIZER_UNICODE61)
@Entity(tableName = "word_definition_fts")
data class WordDefinitionFTS(
    val simplified: String,
    val language: String,
    val definition: String
)

@Fts5(contentEntity = ChineseWordAnnotation::class, tokenizer = FtsOptions.TOKENIZER_UNICODE61)
@Entity(tableName = "chinese_word_annotation_fts")
data class ChineseWordAnnotationFTS(
    @ColumnInfo(name = "a_searchable_text") val searchableText: String,
    @ColumnInfo(name = "a_simplified") val simplified: String,
    val notes: String?,
    val themes: String?
)
