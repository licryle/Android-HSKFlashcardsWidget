package fr.berliat.hskwidget.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import java.text.Normalizer

import java.util.Locale

@Entity(indices = [Index(value = ["searchable_text"])])
data class ChineseWord(
    @PrimaryKey val simplified: String,
    @ColumnInfo(name = "traditional") val traditional: String?,
    @ColumnInfo(name = "definition") val definition: Map<Locale, String>,
    @ColumnInfo(name = "hsk_level") val hskLevel: HSK_Level?,
    @ColumnInfo(name = "pinyins") val pinyins: Pinyins?,
    @ColumnInfo(name = "popularity") val popularity: Int?,
) {
    @ColumnInfo(name = "searchable_text") var searchable_text: String =
        Normalizer.normalize(pinyins.toString() + " " + definition + " "
                + traditional + " " + simplified,
            Normalizer.Form.NFD).replace("\\p{Mn}+".toRegex(), "")

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
                if (findValue == 10)
                    return NOT_HSK
                else
                    return HSK_Level.valueOf("HSK$findValue")
            }
        }
    }
}