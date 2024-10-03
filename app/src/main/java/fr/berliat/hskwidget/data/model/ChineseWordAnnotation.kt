package fr.berliat.hskwidget.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.text.Normalizer
import java.util.Date

@Entity(indices = [Index(value = ["a_searchable_text"])])
data class ChineseWordAnnotation (
    @PrimaryKey @ColumnInfo(name = "a_simplified") val simplified: String,
    @ColumnInfo(name = "a_pinyins") val pinyins: ChineseWord.Pinyins?,
    @ColumnInfo(name = "notes") val notes: String?,
    @ColumnInfo(name = "class_type") val classType: ClassType?,
    @ColumnInfo(name = "class_level") val level: ClassLevel?,
    @ColumnInfo(name = "themes") val themes: String?,

    @ColumnInfo(name = "first_seen") val firstSeen: Date?,
    @ColumnInfo(name = "is_exam") val isExam: Boolean?,
) {
    @ColumnInfo(name = "a_searchable_text", defaultValue = "") var a_searchable_text: String =
        Normalizer.normalize(pinyins.toString() + " " + notes + " " + themes,
            Normalizer.Form.NFD).replace("\\p{Mn}+".toRegex(), "")

    enum class ClassType (val type: String) {
        Speaking("口语"),
        Writing("写作"),
        Reading("精读"),
        FastReading("阅读"),
        NotFromClass("其他");
        companion object {
            infix fun from(findValue: String): ClassType = ClassType.valueOf(findValue)
        }
    }
    enum class ClassLevel (val lvl: String) {
        Elementary1("初一"),
        Elementary2("初二"),
        Elementary3("初三"),
        Elementary4("初四"),
        Intermediate1("中一"),
        Intermediate2("中二"),
        Intermediate3("中三"),
        Advanced1("高一"),
        Advanced2("高二"),
        Advanced3("高三"),
        NotFromClass("其他");
        companion object {
            infix fun from(findValue: String): ClassLevel = ClassLevel.valueOf(findValue)
        }
    }

    companion object {
        fun getBlank(simplified: String = ""): ChineseWordAnnotation {
            return ChineseWordAnnotation(simplified, null, "", ClassType.NotFromClass,
                ClassLevel.NotFromClass, "", null, false)
        }
    }
}