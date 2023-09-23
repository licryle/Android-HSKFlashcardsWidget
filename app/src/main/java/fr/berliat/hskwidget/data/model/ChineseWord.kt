package fr.berliat.hskwidget.data.model

import java.util.Locale

data class ChineseWord(
    val simplified: String,
    val traditional: String,

    val definition: Map<Locale, String>,
    val HSK: HSK_Level,

    val pinyins: Pinyins
) {
    class Pinyins: ArrayList<Pinyin> {

        constructor(s: String) {
            val pinStrings = s.split(" ").toTypedArray()
            val pinyins = ArrayList<Pinyin>()

            var tone : Pinyin.Tone = Pinyin.Tone.NEUTRAL
            pinStrings.forEach { syllable ->
                if (syllable.contains(Regex("[àèìòùǜ]"))) {
                    tone = Pinyin.Tone.FALLING
                } else if (syllable.contains(Regex("[áéíóúǘ]"))) {
                    tone = Pinyin.Tone.RISING
                } else if (syllable.contains(Regex("[ǎěǐǒǔǚ]"))) {
                    tone = Pinyin.Tone.FALLING_RISING
                } else if (syllable.contains(Regex("[āēīōūǖ]"))) {
                    tone = Pinyin.Tone.FLAT
                }

                pinyins.add(Pinyin(syllable, tone))
            }

            this.addAll(pinyins)
        }

        override fun toString(): String {
            var s = ""

            this.forEach() { pinyin ->
                s += pinyin.syllable + ' '
            }

            return s.dropLast(1)
        }
    }

    data class Pinyin (val syllable: String, val tone: Tone) {
        enum class Tone(val toneId: Int) {
            FLAT(1),
            RISING(2),
            FALLING_RISING(3),
            FALLING(4),
            NEUTRAL(5)
        }
    }
    enum class HSK_Level (val level: Int) {
        HSK1(1),
        HSK2(2),
        HSK3(3),
        HSK4(4),
        HSK5(5),
        HSK6(6);
        companion object {
            infix fun from(findValue: Int): HSK_Level = HSK_Level.valueOf("HSK$findValue")
        }
    }
}