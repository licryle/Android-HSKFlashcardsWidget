package fr.berliat.hskwidget.data.model

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(
    tableName = "chinese_word_frequency",
    indices = [Index(value = ["simplified"])])
data class ChineseWordFrequency(
    @PrimaryKey val simplified: String,
    @ColumnInfo(name = "appearance_count") val appearanceCnt: Int = 0,
    @ColumnInfo(name = "consulted_count") val consultedCnt: Int = 0
)