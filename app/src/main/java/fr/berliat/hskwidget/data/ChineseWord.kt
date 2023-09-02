package fr.berliat.hskwidget.data

import java.util.Locale

data class ChineseWord(
    val simplified: String,
    val traditional: String,

    val definition: Map<Locale, String>,

    val pinyins: List<Pinyin>) {
    data class Pinyin (val syllable: String, val tone: Tone) {
        enum class Tone(val toneId: Int) {
            FLAT(1),
            RISING(2),
            FALLING_RISING(3),
            FALLING(4),
            NEUTRAL(5)
        }
    }
}