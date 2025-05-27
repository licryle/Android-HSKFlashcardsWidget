package fr.berliat.hskwidget.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import fr.berliat.hskwidget.data.model.WidgetListEntry

@Dao
interface WidgetListsDAO {
    @Query("SELECT listId FROM widget_list_entries WHERE widgetId = :widgetId")
    suspend fun getListsForWidget(widgetId: Int): List<Long>

    @Query("SELECT * FROM widget_list_entries")
    suspend fun getAllEntries(): List<WidgetListEntry>

    @Insert
    suspend fun insertListToWidget(entry: WidgetListEntry)

    @Insert
    suspend fun insertListsToWidget(entries: List<WidgetListEntry>)

    @Delete
    suspend fun deleteWidgetLists(entries: List<WidgetListEntry>)

    @Query("DELETE FROM widget_list_entries WHERE widgetId = :widgetId")
    suspend fun deleteWidget(widgetId: Int)

    @Query("DELETE FROM widget_list_entries")
    suspend fun deleteAllWidgets()
}