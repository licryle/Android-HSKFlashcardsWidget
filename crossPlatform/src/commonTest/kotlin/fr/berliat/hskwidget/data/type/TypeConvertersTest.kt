package fr.berliat.hskwidget.data.type

import fr.berliat.hskwidget.core.Locale
import fr.berliat.hskwidget.data.model.ChineseWord
import fr.berliat.hskwidget.data.model.ChineseWordAnnotation
import fr.berliat.hskwidget.data.model.WordList
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TypeConvertersTest {

    @Test
    fun testDefinitionsConverter() {
        val definitions = mapOf(
            Locale.ENGLISH to "hello",
            Locale.CHINESE to "你好"
        )
        val stringValue = DefinitionsConverter.fromStringMap(definitions)
        assertNotNull(stringValue)
        
        val decoded = DefinitionsConverter.fromString(stringValue)
        assertEquals(definitions, decoded)
        
        assertEquals(mapOf(), DefinitionsConverter.fromString(null))
        assertNull(DefinitionsConverter.fromStringMap(null))
    }

    @Test
    fun testWordTypeConverter() {
        assertEquals(WordType.NOUN, WordTypeConverter.fromType("NOUN"))
        assertEquals(WordType.UNKNOWN, WordTypeConverter.fromType(null))
        assertEquals(WordType.UNKNOWN, WordTypeConverter.fromType("INVALID"))
        assertEquals("NOUN", WordTypeConverter.toType(WordType.NOUN))
    }

    @Test
    fun testModalityConverter() {
        assertEquals(Modality.ORAL, ModalityConverter.fromModality("ORAL"))
        assertEquals(Modality.UNKNOWN, ModalityConverter.fromModality(null))
        assertEquals("ORAL", ModalityConverter.toModality(Modality.ORAL))
        assertEquals("N/A", ModalityConverter.toModality(Modality.UNKNOWN))
    }

    @Test
    fun testInstantConverter() {
        val now = Instant.fromEpochMilliseconds(1672531200000L) // 2023-01-01
        val millis = InstantConverter.fromInstant(now)
        assertEquals(1672531200000L, millis)
        
        val decoded = InstantConverter.toInstant(millis)
        assertEquals(now, decoded)
        
        assertNull(InstantConverter.fromInstant(null))
        assertNull(InstantConverter.toInstant(null))
    }

    @Test
    fun testListTypeConverter() {
        val converter = ListTypeConverter()
        assertEquals("USER", converter.fromListType(WordList.ListType.USER))
        assertEquals("SYSTEM", converter.fromListType(WordList.ListType.SYSTEM))
        
        assertEquals(WordList.ListType.USER, converter.toListType("USER"))
        assertEquals(WordList.ListType.SYSTEM, converter.toListType("SYSTEM"))
        assertEquals(WordList.ListType.USER, converter.toListType("user"))
    }

    @Test
    fun testAnnotatedChineseWordsConverter() {
        val word = ChineseWord.getBlank("你好")
        val annotation = ChineseWordAnnotation.getBlank("你好")
        
        val map = mapOf(annotation to listOf(word))
        
        // test fromMapToList
        val list = AnnotatedChineseWordsConverter.fromMapToList(map)
        assertEquals(1, list.size)
        assertEquals("你好", list[0].simplified)
        
        // test fromMapToFirst
        val first = AnnotatedChineseWordsConverter.fromMapToFirst(map)
        assertNotNull(first)
        assertEquals("你好", first.simplified)
        assertNull(AnnotatedChineseWordsConverter.fromMapToFirst(emptyMap()))
        
        // test fromListToMap
        // The implementation of fromListToMap in the source seems a bit specific: 
        // it takes List<Map<ChineseWordAnnotation, List<ChineseWord>>>
        val listForMap = listOf(map)
        val resultMap = AnnotatedChineseWordsConverter.fromListToMap(listForMap)
        assertEquals(1, resultMap.size)
        assertTrue(resultMap.containsKey("你好"))
        assertEquals("你好", resultMap["你好"]?.simplified)
    }
}
