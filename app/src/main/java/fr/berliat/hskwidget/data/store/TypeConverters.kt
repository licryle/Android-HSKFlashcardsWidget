package fr.berliat.hskwidget.data.store

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import fr.berliat.hskwidget.data.model.AnnotatedChineseWord
import fr.berliat.hskwidget.data.model.ChineseWord
import fr.berliat.hskwidget.data.model.ChineseWordAnnotation
import java.util.Date
import java.util.Locale


object DefinitionsConverter {
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

object WordTypeConverter {
    @TypeConverter
    @JvmStatic
    fun fromType(value: String?): ChineseWord.Type =
        value?.let { ChineseWord.Type.from(it) } ?: ChineseWord.Type.UNKNOWN

    @TypeConverter
    @JvmStatic
    fun toType(type: ChineseWord.Type): String = type.typ
}

object ModalityConverter {
    @TypeConverter
    @JvmStatic
    fun fromModality(value: String?): ChineseWord.Modality =
        value?.let { ChineseWord.Modality.from(it) } ?: ChineseWord.Modality.UNKNOWN

    @TypeConverter
    @JvmStatic
    fun toModality(modality: ChineseWord.Modality): String = modality.mod
}

object AnnotatedChineseWordsConverter {
    @TypeConverter
    @JvmStatic
    fun fromMapToList(m: Map<ChineseWordAnnotation, List<ChineseWord>>): List<AnnotatedChineseWord> {
        val words = mutableSetOf<AnnotatedChineseWord>()

        m.forEach {
            words.add(AnnotatedChineseWord(it.value[0], it.key))
        }

        return words.toList()
    }

    @TypeConverter
    @JvmStatic
    fun fromMapToFirst(m: Map<ChineseWordAnnotation, List<ChineseWord>>): AnnotatedChineseWord? {
        val words = fromMapToList(m)

        if (words.isEmpty())
            return null

        return words.first()
    }

    @TypeConverter
    @JvmStatic
    fun fromListToMap(l: List<Map<ChineseWordAnnotation, List<ChineseWord>>>): Map<String, AnnotatedChineseWord> {
        val words = mutableMapOf<String, AnnotatedChineseWord>()

        l.forEach {
            words[it.keys.first().simplified] =
                AnnotatedChineseWord(it.values.first()[0], it.keys.first())
        }

        return words
    }
}

object DateConverter {
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