package fr.berliat.hskwidget.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

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