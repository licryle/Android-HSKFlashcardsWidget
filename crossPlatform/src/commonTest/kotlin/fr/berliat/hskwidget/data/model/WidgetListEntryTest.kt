package fr.berliat.hskwidget.data.model

import kotlin.test.Test
import kotlin.test.assertEquals

class WidgetListEntryTest {

    @Test
    fun testWidgetListEntryCreation() {
        val entry = WidgetListEntry(
            widgetId = 1,
            listId = 100L
        )
        
        assertEquals(1, entry.widgetId)
        assertEquals(100L, entry.listId)
    }

    @Test
    fun testCopy() {
        val entry = WidgetListEntry(1, 100L)
        val copy = entry.copy(widgetId = 2)
        
        assertEquals(2, copy.widgetId)
        assertEquals(100L, copy.listId)
    }
}
