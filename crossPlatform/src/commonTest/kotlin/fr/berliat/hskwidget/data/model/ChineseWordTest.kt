package fr.berliat.hskwidget.data.model

import fr.berliat.hskwidget.core.Locale
import fr.berliat.hskwidget.data.type.HSK_Level
import fr.berliat.hskwidget.data.type.Modality
import fr.berliat.hskwidget.data.type.Pinyins
import fr.berliat.hskwidget.data.type.WordType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ChineseWordTest {

    @Test
    fun testConstructorAndProperties() {
        val definition = mapOf(Locale.ENGLISH to "hello")
        val pinyins = Pinyins("nǐ hǎo")
        val word = ChineseWord(
            simplified = "你好",
            traditional = "妳好",
            definition = definition,
            hskLevel = HSK_Level.HSK1,
            pinyins = pinyins,
            popularity = 100,
            examples = "你好吗？",
            modality = Modality.ORAL,
            wordType = WordType.INTERJECTION,
            synonyms = "您好",
            antonym = "再见",
            searchable_text = "nihao hello"
        )

        assertEquals("你好", word.simplified)
        assertEquals("妳好", word.traditional)
        assertEquals(definition, word.definition)
        assertEquals(HSK_Level.HSK1, word.hskLevel)
        assertEquals(pinyins, word.pinyins)
        assertEquals(100, word.popularity)
        assertEquals("你好吗？", word.examples)
        assertEquals(Modality.ORAL, word.modality)
        assertEquals(WordType.INTERJECTION, word.wordType)
        assertEquals("您好", word.synonyms)
        assertEquals("再见", word.antonym)
        assertEquals("nihao hello", word.searchable_text)
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

    @Test
    fun testUpdateSearchable() {
        val word = ChineseWord(
            simplified = "你好",
            traditional = "你好",
            definition = mapOf(Locale.ENGLISH to "hello"),
            hskLevel = HSK_Level.HSK1,
            pinyins = Pinyins("nǐ hǎo"),
            popularity = 1,
            modality = Modality.ORAL,
            wordType = WordType.INTERJECTION
        )
        
        word.updateSearchable()
        
        // nǐ hǎo -> nihao (normalized)
        assertTrue(word.searchable_text.contains("nihao"))
        assertTrue(word.searchable_text.contains("hello"))
        assertTrue(word.searchable_text.contains("你好"))
    }

    @Test
    fun testUpdateSearchableWithNullPinyins() {
        val word = ChineseWord(
            simplified = "你好",
            traditional = "你好",
            definition = mapOf(Locale.ENGLISH to "hello"),
            hskLevel = HSK_Level.HSK1,
            pinyins = null,
            popularity = 1
        )
        
        word.updateSearchable()
        
        // If it's buggy (uses pinyins.toString()), searchable_text will contain "null"
        assertFalse(word.searchable_text.contains("null"), "searchable_text should not contain the word 'null' when pinyins is null")
    }
}
