package fr.berliat.hskwidget.data.store

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import fr.berliat.hskwidget.data.dao.AnnotatedChineseWord
import fr.berliat.hskwidget.data.model.ChineseWord
import fr.berliat.hskwidget.data.model.ChineseWordAnnotation
import java.util.Date
import java.util.Locale

class TypeConverters {
    class DefinitionsConverter {
        companion object {
            @TypeConverter
            @JvmStatic
            fun fromStringMap(value: Map<Locale, String>?): String? {
                return Gson().toJson(value)
            }

            @TypeConverter
            @JvmStatic
            fun fromString(s: String?): Map<Locale, String>? {
                if (s == null)
                    return mapOf<Locale, String>()

                val mapType = object : TypeToken<Map<Locale, String>>() {}.type
                return Gson().fromJson(s, mapType)
            }
        }
    }

    class AnnotatedChineseWordsConverter {
        companion object {
            @TypeConverter
            @JvmStatic
            fun fromMap(m: Map<ChineseWordAnnotation, List<ChineseWord>>): List<AnnotatedChineseWord> {
                val words = mutableSetOf<AnnotatedChineseWord>()

                m.forEach {
                    words.add(AnnotatedChineseWord(it.value[0], it.key))
                }

                return words.toList()
            }
        }
    }

    class DateConverter {
        companion object {
            @TypeConverter
            @JvmStatic
            fun toDate(dateLong: Long?): Date? {
                return if (dateLong == null) null else Date(dateLong)
            }

            @TypeConverter
            @JvmStatic
            fun fromDate(date: Date?): Long? {
                return if (date == null) null else date.getTime()
            }
        }
    }
}