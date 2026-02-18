package fr.berliat.hskwidget.data.type

import kotlin.test.Test
import kotlin.test.assertEquals

class DataTypesTest {

    @Test
    fun testClassLevel() {
        assertEquals("初一 / Elementary1", ClassLevel.Elementary1.toString())
        assertEquals(ClassLevel.Elementary1, ClassLevel from "Elementary1")
        assertEquals(ClassLevel.NotFromClass, ClassLevel from "NotFromClass")
    }

    @Test
    fun testClassType() {
        assertEquals("口语 / Speaking", ClassType.Speaking.toString())
        assertEquals(ClassType.Speaking, ClassType from "Speaking")
        assertEquals(ClassType.NotFromClass, ClassType from "NotFromClass")
    }

    @Test
    fun testHSKLevel() {
        assertEquals(HSK_Level.HSK1, HSK_Level.from(1))
        assertEquals(HSK_Level.HSK6, HSK_Level.from(6))
        assertEquals(HSK_Level.HSK9, HSK_Level.from(9))
        assertEquals(HSK_Level.NOT_HSK, HSK_Level.from(10))
    }

    @Test
    fun testModality() {
        assertEquals("ORAL", Modality.ORAL.toString())
        assertEquals("ORAL & WRITTEN", Modality.ORAL_WRITTEN.toString())
        assertEquals(Modality.ORAL, Modality.from("ORAL"))
        assertEquals(Modality.UNKNOWN, Modality.from("INVALID"))
        assertEquals(Modality.UNKNOWN, Modality.from("N/A"))
        assertEquals("N/A", Modality.UNKNOWN.mod)
    }

    @Test
    fun testWordType() {
        assertEquals(WordType.NOUN, WordType.from("NOUN"))
        assertEquals(WordType.UNKNOWN, WordType.from("INVALID"))
        assertEquals(WordType.UNKNOWN, WordType.from("N/A"))
        assertEquals("NOUN", WordType.NOUN.wordType)
    }

    @Test
    fun testPinyin() {
        val pinyin = Pinyin("ma", Pinyin.Tone.FALLING)
        assertEquals("ma", pinyin.syllable)
        assertEquals(Pinyin.Tone.FALLING, pinyin.tone)
        assertEquals(4, Pinyin.Tone.FALLING.toneId)
    }

    @Test
    fun testPinyins() {
        val pinyins = Pinyins.fromString("mā má mǎ mà ma")
        assertEquals(5, pinyins.size)
        assertEquals(Pinyin.Tone.FLAT, pinyins[0].tone)
        assertEquals(Pinyin.Tone.RISING, pinyins[1].tone)
        assertEquals(Pinyin.Tone.FALLING_RISING, pinyins[2].tone)
        assertEquals(Pinyin.Tone.FALLING, pinyins[3].tone)
        assertEquals(Pinyin.Tone.NEUTRAL, pinyins[4].tone)

        assertEquals("mā má mǎ mà ma", pinyins.toString())
        
        val pinyinsWithU = Pinyins.fromString("lǘ lǜ lǖ lǚ")
        assertEquals(4, pinyinsWithU.size)
        assertEquals(Pinyin.Tone.RISING, pinyinsWithU[0].tone)
        assertEquals(Pinyin.Tone.FALLING, pinyinsWithU[1].tone)
        assertEquals(Pinyin.Tone.FLAT, pinyinsWithU[2].tone)
        assertEquals(Pinyin.Tone.FALLING_RISING, pinyinsWithU[3].tone)
        
        val emptyPinyins = Pinyins.fromString("")
        assertEquals(0, emptyPinyins.size)
        
        val nullPinyins = Pinyins.fromString(null)
        assertEquals(0, nullPinyins.size)
    }

    @Test
    fun testPinyinsConstructor() {
        val pinyins = Pinyins("nǐ hǎo")
        assertEquals(2, pinyins.size)
        assertEquals("nǐ", pinyins[0].syllable)
        assertEquals(Pinyin.Tone.FALLING_RISING, pinyins[0].tone)
        assertEquals("hǎo", pinyins[1].syllable)
        assertEquals(Pinyin.Tone.FALLING_RISING, pinyins[1].tone)
    }
}
