package fr.berliat.hskwidget.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

import java.util.Locale

@Entity(indices = [Index(value = ["simplified"])])
data class ChineseWordFrequency(
    @PrimaryKey val simplified: String,
    @ColumnInfo(name = "appearance_count") val appearanceCnt: Int = 0,
    @ColumnInfo(name = "consulted_count") val consultedCnt: Int = 0
)