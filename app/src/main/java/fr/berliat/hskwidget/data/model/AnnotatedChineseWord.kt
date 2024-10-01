package fr.berliat.hskwidget.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity
data class AnnotatedChineseWord (
    @PrimaryKey val simplified: String,
    @ColumnInfo(name = "pinyins") val pinyins: ChineseWord.Pinyins?,
    @ColumnInfo(name = "notes") val notes: String?,
    @ColumnInfo(name = "class") val classType: Class?,
    @ColumnInfo(name = "level") val level: Class?,
    @ColumnInfo(name = "themes") val themes: String?,

    @ColumnInfo(name = "first_seen") val firstSeen: Date?,
    @ColumnInfo(name = "is_exam") val isExam: Boolean?,
) {
    enum class Class (val type: String) {
        Speaking("口语"),
        Writing("写作"),
        Reading("精读"),
        FastReading("阅读"),
        NotFromClass("其他");
        companion object {
            infix fun from(findValue: String): Class = Class.valueOf(findValue)
        }
    }
    enum class Level (val lvl: String) {
        Elementary1("初一"),
        Elementary2("初二"),
        Elementary3("初三"),
        Elementary4("初四"),
        Intermediate1("中一"),
        Intermediate2("中二"),
        Intermediate3("中三"),
        Advanced1("高一"),
        Advanced2("高二"),
        Advanced3("高三");
        companion object {
            infix fun from(findValue: String): Level = Level.valueOf(findValue)
        }
    }
}