package fr.berliat.hskwidget.data.model

import fr.berliat.hskwidget.data.type.HSK_Level
import fr.berliat.hskwidget.data.type.Modality
import fr.berliat.hskwidget.data.type.Pinyins
import fr.berliat.hskwidget.data.type.WordType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ChineseWordTest {

    @Test
    fun testConstructorAndProperties() {
        val pinyins = Pinyins("nǐ hǎo")
        val word = ChineseWord(
            simplified = "你好",
            traditional = "妳好",
            hskLevel = HSK_Level.HSK1,
            pinyins = pinyins,
            popularity = 100,
            examples = "你好吗？",
            collocations = "你好, 您好",
            modality = Modality.ORAL,
            wordType = WordType.INTERJECTION,
            synonyms = "您好",
            antonym = "再见",
        )

        assertEquals("你好", word.simplified)
        assertEquals("妳好", word.traditional)
        assertEquals(HSK_Level.HSK1, word.hskLevel)
        assertEquals(pinyins, word.pinyins)
        assertEquals(100, word.popularity)
        assertEquals("你好吗？", word.examples)
        assertEquals("你好, 您好", word.collocations)
        assertEquals(Modality.ORAL, word.modality)
        assertEquals(WordType.INTERJECTION, word.wordType)
        assertEquals("您好", word.synonyms)
        assertEquals("再见", word.antonym)
    }

    @Test
    fun testConstructorThrowsOnEmptySimplified() {
        assertFailsWith<WordMissingSimplifiedException> {
            ChineseWord(
                simplified = "",
                traditional = null,
                hskLevel = null,
                pinyins = null,
                popularity = null
            )
        }
        
        assertFailsWith<WordMissingSimplifiedException> {
            ChineseWord.getBlank("")
        }
    }

    @Test
    fun testGetBlank() {
        val word = ChineseWord.getBlank("你好")
        assertEquals("你好", word.simplified)
        assertEquals("", word.traditional)
        assertTrue(word.definition.isEmpty())
        assertNull(word.hskLevel)
        assertNull(word.pinyins)
        assertEquals(Modality.UNKNOWN, word.modality)
        assertEquals(WordType.UNKNOWN, word.wordType)
    }
}
