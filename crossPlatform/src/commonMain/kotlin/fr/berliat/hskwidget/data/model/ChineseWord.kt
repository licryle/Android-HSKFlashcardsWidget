package fr.berliat.hskwidget.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

import doist.x.normalize.Form
import doist.x.normalize.normalize

import fr.berliat.hskwidget.core.Locale
import fr.berliat.hskwidget.data.type.HSK_Level
import fr.berliat.hskwidget.data.type.Modality
import fr.berliat.hskwidget.data.type.Pinyins
import fr.berliat.hskwidget.data.type.WordType
import kotlinx.serialization.Serializable

class WordMissingSimplifiedException(message: String = "Word must have a non null or empty simplified") : Exception(message)

@Serializable
@Entity(
    tableName = "chinese_word",
    indices = [Index(value = ["searchable_text"])])
data class ChineseWord(
    // @Todo: lots of fields should be not-null. After hours of research, I can't get past compilation errors. So someday...
    @PrimaryKey val simplified: String,
    @ColumnInfo(name = "traditional") val traditional: String?,
    @ColumnInfo(name = "definition") val definition: Map<Locale, String>,
    @ColumnInfo(name = "hsk_level") val hskLevel: HSK_Level?,
    @ColumnInfo(name = "pinyins") val pinyins: Pinyins?,
    @ColumnInfo(name = "popularity") val popularity: Int?,
    @ColumnInfo(name = "examples", defaultValue = "") val examples: String? = "",
    @ColumnInfo(name = "modality", defaultValue = "N/A") val modality: Modality? = Modality.UNKNOWN,
    @ColumnInfo(name = "type", defaultValue = "N/A") val wordType: WordType? = WordType.UNKNOWN,
    @ColumnInfo(name = "synonyms", defaultValue = "") val synonyms: String? = "",
    @ColumnInfo(name = "antonym", defaultValue = "") val antonym: String? = "",
    @ColumnInfo(name = "searchable_text", defaultValue = "") var searchable_text: String = ""
) {
    init {
        if (simplified.isBlank()) {
            throw WordMissingSimplifiedException()
        }
    }

    fun updateSearchable() {
        val cleanPinyins = Pinyins.toString(pinyins).replace(" ", "")
        searchable_text = "$cleanPinyins $definition $traditional $simplified"
            .normalize(Form.NFD).replace("\\p{Mn}+".toRegex(), "")
    }

    companion object {
        fun getBlank(simplified: String = ""): ChineseWord {
            return ChineseWord(simplified, "", mapOf<Locale, String>(), null,
                null, null, "", Modality.UNKNOWN, WordType.UNKNOWN,
                "", "", "")
        }
    }
}
