package fr.berliat.hskwidget.data.type

import androidx.room.TypeConverter
import kotlin.jvm.JvmStatic

class Pinyins(
    private val items: MutableList<Pinyin> = mutableListOf()
) : MutableList<Pinyin> by items {

    constructor(s: String) : this(fromString(s).items)

    override fun toString(): String = toString(this)

    companion object {
        @TypeConverter
        @JvmStatic
        fun fromString(value: String?): Pinyins {
            if (value.isNullOrBlank()) return Pinyins()

            val pinyins = mutableListOf<Pinyin>()
            val pinStrings = value.split(" ")

            pinStrings.forEach { syllable ->
                val tone = when {
                    syllable.contains(Regex("[àèìòùǜ]")) -> Pinyin.Tone.FALLING
                    syllable.contains(Regex("[áéíóúǘ]")) -> Pinyin.Tone.RISING
                    syllable.contains(Regex("[ǎěǐǒǔǚ]")) -> Pinyin.Tone.FALLING_RISING
                    syllable.contains(Regex("[āēīōūǖ]")) -> Pinyin.Tone.FLAT
                    else -> Pinyin.Tone.NEUTRAL
                }
                pinyins.add(Pinyin(syllable, tone))
            }

            return Pinyins(pinyins)
        }

        @TypeConverter
        @JvmStatic
        fun toString(pinyins: Pinyins?): String {
            if (pinyins == null) return ""
            return pinyins.joinToString(" ") { it.syllable }
        }
    }
}