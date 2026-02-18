package fr.berliat.hskwidget.data.model

import kotlinx.datetime.Clock
import kotlin.test.Test
import kotlin.test.assertEquals

class WordListTest {

    @Test
    fun testWordListCreation() {
        val now = Clock.System.now()
        val wordList = WordList(
            name = "My List",
            creationDate = now,
            lastModified = now,
            listType = WordList.ListType.USER
        )
        
        assertEquals("My List", wordList.name)
        assertEquals(now, wordList.creationDate)
        assertEquals(now, wordList.lastModified)
        assertEquals(WordList.ListType.USER, wordList.listType)
    }

    @Test
    fun testListTypeEnum() {
        assertEquals(WordList.ListType.USER, WordList.ListType from "USER")
        assertEquals(WordList.ListType.SYSTEM, WordList.ListType from "SYSTEM")
    }

    @Test
    fun testWordListWithCount() {
        val now = Clock.System.now()
        val withCount = WordListWithCount(
            name = "Test",
            id = 1L,
            creationDate = now,
            lastModified = now,
            ankiDeckId = 123L,
            listType = WordList.ListType.SYSTEM,
            wordCount = 10
        )
        
        assertEquals(10, withCount.wordCount)
        val list = withCount.wordList
        assertEquals("Test", list.name)
        assertEquals(1L, list.id)
        assertEquals(123L, list.ankiDeckId)
        assertEquals(WordList.ListType.SYSTEM, list.listType)
    }

    @Test
    fun testConstants() {
        assertEquals(0L, WordList.ANKI_ID_EMPTY)
        assertEquals("Annotated Words", WordList.SYSTEM_ANNOTATED_NAME)
    }
}
