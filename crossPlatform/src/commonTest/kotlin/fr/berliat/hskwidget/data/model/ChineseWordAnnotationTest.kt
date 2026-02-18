package fr.berliat.hskwidget.data.model

import fr.berliat.hskwidget.data.type.ClassLevel
import fr.berliat.hskwidget.data.type.ClassType
import fr.berliat.hskwidget.data.type.Pinyins
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ChineseWordAnnotationTest {

    @Test
    fun testConstructorAndProperties() {
        val pinyins = Pinyins("nǐ hǎo")
        val firstSeen = Instant.fromEpochMilliseconds(1672531200000L)
        val annotation = ChineseWordAnnotation(
            simplified = "你好",
            pinyins = pinyins,
            notes = "important note",
            classType = ClassType.Speaking,
            level = ClassLevel.Elementary1,
            themes = "greeting",
            firstSeen = firstSeen,
            isExam = true
        )
        annotation.a_searchable_text = "nihao greeting"

        assertEquals("你好", annotation.simplified)
        assertEquals(pinyins, annotation.pinyins)
        assertEquals("important note", annotation.notes)
        assertEquals(ClassType.Speaking, annotation.classType)
        assertEquals(ClassLevel.Elementary1, annotation.level)
        assertEquals("greeting", annotation.themes)
        assertEquals(firstSeen, annotation.firstSeen)
        assertEquals(true, annotation.isExam)
        assertEquals("nihao greeting", annotation.a_searchable_text)
    }

    @Test
    fun testGetBlank() {
        val annotation = ChineseWordAnnotation.getBlank("你好")
        assertEquals("你好", annotation.simplified)
        assertNull(annotation.pinyins)
        assertEquals("", annotation.notes)
        assertEquals(ClassType.NotFromClass, annotation.classType)
        assertEquals(ClassLevel.NotFromClass, annotation.level)
        assertNull(annotation.firstSeen)
        assertEquals(false, annotation.isExam)
    }

    @Test
    fun testUpdateSearchable() {
        val annotation = ChineseWordAnnotation(
            simplified = "你好",
            pinyins = Pinyins("nǐ hǎo"),
            notes = "important note",
            classType = ClassType.Speaking,
            level = ClassLevel.Elementary1,
            themes = "greeting",
            firstSeen = Instant.fromEpochMilliseconds(0),
            isExam = true
        )
        
        annotation.updateSearchable()
        
        assertTrue(annotation.a_searchable_text.contains("nihao"))
        assertTrue(annotation.a_searchable_text.contains("important note"))
        assertTrue(annotation.a_searchable_text.contains("greeting"))
        assertTrue(annotation.a_searchable_text.contains("你好"))
    }

    @Test
    fun testUpdateSearchableWithNullPinyins() {
        val annotation = ChineseWordAnnotation(
            simplified = "你好",
            pinyins = null,
            notes = "note",
            classType = null,
            level = null,
            themes = "theme",
            firstSeen = null,
            isExam = null
        )
        
        annotation.updateSearchable()
        
        assertFalse(annotation.a_searchable_text.contains("null"), "a_searchable_text should not contain the word 'null' when pinyins is null")
    }
}
