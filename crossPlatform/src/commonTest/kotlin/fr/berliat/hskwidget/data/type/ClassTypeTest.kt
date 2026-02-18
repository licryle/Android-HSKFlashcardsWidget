package fr.berliat.hskwidget.data.type

import kotlin.test.Test
import kotlin.test.assertEquals

class ClassTypeTest {

    @Test
    fun testClassType() {
        assertEquals("口语 / Speaking", ClassType.Speaking.toString())
        assertEquals(ClassType.Speaking, ClassType from "Speaking")
        assertEquals(ClassType.NotFromClass, ClassType from "NotFromClass")
    }
}
