package fr.berliat.hskwidget.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import java.text.Normalizer

import java.util.Locale

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
        searchable_text = Normalizer.normalize("$cleanPinyins $definition $traditional $simplified",
            Normalizer.Form.NFD).replace("\\p{Mn}+".toRegex(), "")
    }

    class Pinyins: ArrayList<Pinyin> {
        constructor(s: String) : this(fromString(s)) {
        }

        constructor(pinyins: ArrayList<Pinyin>) {
            this.addAll(pinyins)
        }

        override fun toString() = toString(this)

        companion object {
            @TypeConverter
            @JvmStatic
            fun fromString(value: String?): Pinyins {
                val pinyins = ArrayList<Pinyin>()
                if (value == null)
                    return Pinyins(pinyins)

                val pinStrings = value.split(" ").toTypedArray()
                var tone: Pinyin.Tone = Pinyin.Tone.NEUTRAL
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

                return Pinyins(pinyins)
            }

            @TypeConverter
            @JvmStatic
            fun toString(pinyins: Pinyins?): String {
                if (pinyins == null) return ""

                var s = ""

                pinyins.forEach() { pinyin ->
                    s += pinyin.syllable + ' '
                }

                return s.dropLast(1)
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
                } catch (e: Exception) {
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
                } catch (e: Exception) {
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

        val CN_HSK3 : Locale = Locale.Builder().setLanguage("zh").setRegion("CN").setVariant("HSK03").build()
    }
}