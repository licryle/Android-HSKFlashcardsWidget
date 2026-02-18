package fr.berliat.hskwidget.data.type

import kotlin.test.Test
import kotlin.test.assertEquals

class WordTypeTest {

    @Test
    fun testWordType() {
        assertEquals(WordType.NOUN, WordType.from("NOUN"))
        assertEquals(WordType.UNKNOWN, WordType.from("INVALID"))
        assertEquals(WordType.UNKNOWN, WordType.from("N/A"))
        assertEquals("NOUN", WordType.NOUN.wordType)
    }
}
