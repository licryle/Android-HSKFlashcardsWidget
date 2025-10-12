package fr.berliat.hskwidget.data.type

import kotlinx.serialization.Serializable

@Serializable
data class Pinyin (val syllable: String, val tone: Tone) {
    enum class Tone(val toneId: Int) {
        FLAT(1),
        RISING(2),
        FALLING_RISING(3),
        FALLING(4),
        NEUTRAL(5)
    }
}