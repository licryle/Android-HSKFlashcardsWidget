package fr.berliat.hskwidget.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

import doist.x.normalize.Form
import doist.x.normalize.normalize
import fr.berliat.hskwidget.data.type.ClassLevel
import fr.berliat.hskwidget.data.type.ClassType
import fr.berliat.hskwidget.data.type.Pinyins
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "chinese_word_annotation",
    indices = [Index(value = ["a_searchable_text"])])
data class ChineseWordAnnotation (
    @PrimaryKey @ColumnInfo(name = "a_simplified") val simplified: String = "",
    @ColumnInfo(name = "a_pinyins") val pinyins: Pinyins?,
    @ColumnInfo(name = "notes") val notes: String?,
    @ColumnInfo(name = "class_type") val classType: ClassType?,
    @ColumnInfo(name = "class_level") val level: ClassLevel?,
    @ColumnInfo(name = "themes") val themes: String?,

    @ColumnInfo(name = "first_seen") val firstSeen: Instant?,
    @ColumnInfo(name = "is_exam") val isExam: Boolean?
) {
    @ColumnInfo(name = "a_searchable_text", defaultValue = "") var a_searchable_text: String = ""

    fun updateSearchable() {
        val cleanPinyins = Pinyins.toString(pinyins).replace(" ", "")
        a_searchable_text = "$cleanPinyins $notes $themes $simplified".normalize(Form.NFD)
            .replace("\\p{Mn}+".toRegex(), "")
    }

    companion object {
        fun getBlank(simplified: String = ""): ChineseWordAnnotation {
            return ChineseWordAnnotation(simplified, null, "", ClassType.NotFromClass,
                ClassLevel.NotFromClass, "", null, false)
        }
    }
}