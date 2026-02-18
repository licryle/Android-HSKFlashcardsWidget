package fr.berliat.hskwidget.data.type

import kotlin.test.Test
import kotlin.test.assertEquals

class PinyinsTest {

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
