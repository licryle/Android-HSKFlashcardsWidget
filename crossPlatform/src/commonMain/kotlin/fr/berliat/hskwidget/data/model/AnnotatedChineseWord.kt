package fr.berliat.hskwidget.data.model

import androidx.room.Embedded
import fr.berliat.hskwidget.core.Locale
import fr.berliat.hskwidget.data.type.HSK_Level
import fr.berliat.hskwidget.data.type.Pinyins
import kotlinx.serialization.Serializable
import kotlin.time.ExperimentalTime

@Serializable
data class AnnotatedChineseWord (
    @Embedded val word: ChineseWord?,
    @Embedded val annotation: ChineseWordAnnotation?) {

    val simplified: String
        get() {
            if (hasWord()) {
                return word?.simplified ?: throw WordMissingSimplifiedException()
            }
            return annotation?.simplified ?: throw WordMissingSimplifiedException()
        }

    val pinyins: Pinyins
        get() {
            return if (hasWord()) {
                word?.pinyins!!
            } else {
                annotation?.pinyins ?: Pinyins()
            }
        }

    val hskLevel: HSK_Level
        get() {
            return if (hasWord()) {
                word?.hskLevel!!
            } else {
                HSK_Level.NOT_HSK
            }
        }

    companion object {
        fun getBlank(simplified: String = ""): AnnotatedChineseWord {
            return AnnotatedChineseWord(
                ChineseWord.getBlank(simplified),
                ChineseWordAnnotation.getBlank(simplified)
            )
        }
    }

    @OptIn(ExperimentalTime::class)
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
                HSK_Level.NOT_HSK,
                Pinyins(""),
                0)
        }

        return null
    }
}
