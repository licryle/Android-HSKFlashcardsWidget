package fr.berliat.hskwidget.data.model

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index

@Entity(
    tableName = "widget_list_entry",
    primaryKeys = ["widget_id", "list_id"],
    foreignKeys = [
        ForeignKey(
            entity = WordList::class,
            parentColumns = ["id"],
            childColumns = ["list_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("widget_id"), Index("list_id")]
)
data class WidgetListEntry(
    @ColumnInfo(name = "widget_id") val widgetId: Int,
    @ColumnInfo(name = "list_id") val listId: Long
)