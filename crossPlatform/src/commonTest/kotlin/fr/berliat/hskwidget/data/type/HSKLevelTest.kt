package fr.berliat.hskwidget.data.type

import kotlin.test.Test
import kotlin.test.assertEquals

class HSKLevelTest {

    @Test
    fun testHSKLevel() {
        assertEquals(HSK_Level.HSK1, HSK_Level.from(1))
        assertEquals(HSK_Level.HSK6, HSK_Level.from(6))
        assertEquals(HSK_Level.HSK9, HSK_Level.from(9))
        assertEquals(HSK_Level.NOT_HSK, HSK_Level.from(10))
    }
}
