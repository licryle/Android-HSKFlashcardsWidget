package fr.berliat.hskwidget.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "widget_list_entries",
    primaryKeys = ["widgetId", "listId"],
    foreignKeys = [
        ForeignKey(
            entity = WordList::class,
            parentColumns = ["id"],
            childColumns = ["listId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("widgetId"), Index("listId")]
)
data class WidgetListEntry(
    val widgetId: Int,
    val listId: Long
)
