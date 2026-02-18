package fr.berliat.hskwidget.data.type

import kotlin.test.Test
import kotlin.test.assertEquals

class ModalityTest {

    @Test
    fun testModality() {
        assertEquals("ORAL", Modality.ORAL.toString())
        assertEquals("ORAL & WRITTEN", Modality.ORAL_WRITTEN.toString())
        assertEquals(Modality.ORAL, Modality.from("ORAL"))
        assertEquals(Modality.UNKNOWN, Modality.from("INVALID"))
        assertEquals(Modality.UNKNOWN, Modality.from("N/A"))
        assertEquals("N/A", Modality.UNKNOWN.mod)
    }
}
