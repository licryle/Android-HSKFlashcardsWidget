package fr.berliat.hskwidget.data.type

import kotlin.test.Test
import kotlin.test.assertEquals

class PinyinTest {

    @Test
    fun testPinyin() {
        val pinyin = Pinyin("ma", Pinyin.Tone.FALLING)
        assertEquals("ma", pinyin.syllable)
        assertEquals(Pinyin.Tone.FALLING, pinyin.tone)
        assertEquals(4, Pinyin.Tone.FALLING.toneId)
    }
}
