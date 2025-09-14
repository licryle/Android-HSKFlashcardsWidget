package fr.berliat.hskwidget.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverter

import doist.x.normalize.Form
import doist.x.normalize.normalize

import fr.berliat.hskwidget.core.Locale
import kotlin.jvm.JvmStatic

@Entity(
    tableName = "chinese_word",
    indices = [Index(value = ["searchable_text"])])
data class ChineseWord(
    // @Todo: lots of fields should be not-null. After hours of research, I can't get past compilation errors. So someday...
    @PrimaryKey val simplified: String,
    @ColumnInfo(name = "traditional") val traditional: String?,
    @ColumnInfo(name = "definition") val definition: Map<Locale, String>,
    @ColumnInfo(name = "hsk_level") val hskLevel: HSK_Level?,
    @ColumnInfo(name = "pinyins") val pinyins: Pinyins?,
    @ColumnInfo(name = "popularity") val popularity: Int?,
    @ColumnInfo(name = "examples", defaultValue = "") val examples: String? = "",
    @ColumnInfo(name = "modality", defaultValue = "N/A") val modality: Modality? = Modality.UNKNOWN,
    @ColumnInfo(name = "type", defaultValue = "N/A") val type: Type? = Type.UNKNOWN,
    @ColumnInfo(name = "synonyms", defaultValue = "") val synonyms: String? = "",
    @ColumnInfo(name = "antonym", defaultValue = "") val antonym: String? = "",
    @ColumnInfo(name = "searchable_text", defaultValue = "") var searchable_text: String = ""
) {

    fun updateSearchable() {
        val cleanPinyins = pinyins.toString().replace(" ", "")
        searchable_text = "$cleanPinyins $definition $traditional $simplified"
            .normalize(Form.NFD).replace("\\p{Mn}+".toRegex(), "")
    }

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
        HSK6(6),
        HSK7(7),
        HSK8(8),
        HSK9(9),
        NOT_HSK(10);
        companion object {
            fun from(findValue: Int): HSK_Level {
                return if (findValue == 10)
                    NOT_HSK
                else
                    HSK_Level.valueOf("HSK$findValue")
            }
        }
    }

    enum class Modality(val mod: String) {
        ORAL("ORAL"),
        WRITTEN("WRITTEN"),
        ORAL_WRITTEN("ORAL_WRITTEN"),
        UNKNOWN("N/A");

        companion object {
            fun from(findValue: String): Modality {
                return try {
                    Modality.valueOf(findValue)
                } catch (_: Exception) {
                    UNKNOWN
                }
            }
        }
    }

    enum class Type(val typ: String) {
        NOUN("NOUN"),
        VERB("VERB"),
        ADJECTIVE("ADJECTIVE"),
        ADVERB("ADVERB"),
        CONJUNCTION("CONJUNCTION"),
        PREPOSITION("PREPOSITION"),
        INTERJECTION("INTERJECTION"),
        IDIOM("IDIOM"),
        UNKNOWN("N/A");

        companion object {
            fun from(findValue: String): Type {
                return try {
                    Type.valueOf(findValue)
                } catch (_: Exception) {
                    UNKNOWN
                }
            }
        }
    }

    companion object {
        fun getBlank(simplified: String = ""): ChineseWord {
            return ChineseWord(simplified, "", mapOf<Locale, String>(), null,
                null, null, "", Modality.UNKNOWN, Type.UNKNOWN,
                "", "", "")
        }
    }
}