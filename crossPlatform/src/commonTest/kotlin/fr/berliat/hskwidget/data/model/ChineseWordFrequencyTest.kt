package fr.berliat.hskwidget.data.model

import kotlin.test.Test
import kotlin.test.assertEquals

class ChineseWordFrequencyTest {

    @Test
    fun testChineseWordFrequencyCreation() {
        val freq = ChineseWordFrequency(
            simplified = "测试",
            appearanceCnt = 5,
            consultedCnt = 2
        )
        
        assertEquals("测试", freq.simplified)
        assertEquals(5, freq.appearanceCnt)
        assertEquals(2, freq.consultedCnt)
    }

    @Test
    fun testDefaultValues() {
        val freq = ChineseWordFrequency("默认")
        assertEquals(0, freq.appearanceCnt)
        assertEquals(0, freq.consultedCnt)
    }
}
