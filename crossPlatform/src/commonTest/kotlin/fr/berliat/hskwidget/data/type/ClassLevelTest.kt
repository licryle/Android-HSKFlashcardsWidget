package fr.berliat.hskwidget.data.type

import kotlin.test.Test
import kotlin.test.assertEquals

class ClassLevelTest {

    @Test
    fun testClassLevel() {
        assertEquals("初一 / Elementary1", ClassLevel.Elementary1.toString())
        assertEquals(ClassLevel.Elementary1, ClassLevel from "Elementary1")
        assertEquals(ClassLevel.NotFromClass, ClassLevel from "NotFromClass")
    }
}
