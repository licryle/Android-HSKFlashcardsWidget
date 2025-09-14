package fr.berliat.hskwidget.data.model

import androidx.room.Embedded
import fr.berliat.hskwidget.core.Locale

data class AnnotatedChineseWord (
    @Embedded val word: ChineseWord?,
    @Embedded val annotation: ChineseWordAnnotation?) {

    val simplified: String
        get() {
            return word?.simplified ?: annotation?.simplified!!
        }

    companion object {
        fun getBlank(simplified: String = ""): AnnotatedChineseWord {
            return AnnotatedChineseWord(
                ChineseWord.getBlank(simplified),
                ChineseWordAnnotation.getBlank(simplified)
            )
        }
    }

    fun hasAnnotation(): Boolean {
        return annotation?.firstSeen != null
    }

    fun hasWord(): Boolean {
        return word?.hskLevel != null
    }

    fun toChineseWord(): ChineseWord? {
        if (hasWord()) return word!!

        if (hasAnnotation()) {
            return ChineseWord(simplified, "",
                mapOf(Pair<Locale,String>(Locale.ENGLISH, annotation?.notes ?: "")),
                ChineseWord.HSK_Level.NOT_HSK,
                ChineseWord.Pinyins(""),
                0)
        }

        return null
    }
}