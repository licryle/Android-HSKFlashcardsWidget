package fr.berliat.hskwidget.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey

import fr.berliat.hskwidget.data.type.HSK_Level
import fr.berliat.hskwidget.data.type.Modality
import fr.berliat.hskwidget.data.type.Pinyins
import fr.berliat.hskwidget.data.type.WordType
import kotlinx.serialization.Serializable

class WordMissingSimplifiedException(message: String = "Word must have a non null or empty simplified") : Exception(message)

@Serializable
@Entity(tableName = "chinese_word")
data class ChineseWord(
    // @Todo: lots of fields should be not-null. After hours of research, I can't get past compilation errors. So someday...
    @PrimaryKey val simplified: String,
    @ColumnInfo(name = "traditional") val traditional: String?,
    @ColumnInfo(name = "hsk_level") val hskLevel: HSK_Level?,
    @ColumnInfo(name = "pinyins") val pinyins: Pinyins?,
    @ColumnInfo(name = "popularity") val popularity: Int?,
    @ColumnInfo(name = "examples", defaultValue = "") val examples: String? = "",
    @ColumnInfo(name = "collocations", defaultValue = "") val collocations: String? = "",
    @ColumnInfo(name = "modality", defaultValue = "N/A") val modality: Modality? = Modality.UNKNOWN,
    @ColumnInfo(name = "type", defaultValue = "N/A") val wordType: WordType? = WordType.UNKNOWN,
    @ColumnInfo(name = "synonyms", defaultValue = "") val synonyms: String? = "",
    @ColumnInfo(name = "antonym", defaultValue = "") val antonym: String? = ""
) {
    /** Hydrated by read DAOs; never persisted in chinese_word. */
    @Ignore var definition: Map<fr.berliat.hskwidget.core.Locale, String> = emptyMap()

    init {
        if (simplified.isBlank()) {
            throw WordMissingSimplifiedException()
        }
    }

    companion object {
        fun getBlank(simplified: String = ""): ChineseWord {
            return ChineseWord(simplified, "", null,
                null, null, "", "", Modality.UNKNOWN, WordType.UNKNOWN,
                "", "")
        }
    }
}
