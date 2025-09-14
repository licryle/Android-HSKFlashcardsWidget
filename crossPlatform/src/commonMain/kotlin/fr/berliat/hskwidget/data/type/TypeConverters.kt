package fr.berliat.hskwidget.data.type

import androidx.room.TypeConverter
import fr.berliat.hskwidget.core.Locale
import fr.berliat.hskwidget.data.model.AnnotatedChineseWord
import fr.berliat.hskwidget.data.model.ChineseWord
import fr.berliat.hskwidget.data.model.ChineseWordAnnotation
import fr.berliat.hskwidget.data.model.WordList
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.jvm.JvmStatic
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

import kotlinx.serialization.json.Json

object DefinitionsConverter {
    @TypeConverter
    @JvmStatic
    fun fromStringMap(value: Map<Locale, String>?): String? {
        return Json.encodeToString(value)
    }

    @TypeConverter
    @JvmStatic
    fun fromString(s: String?): Map<Locale, String>? {
        if (s == null)
            return mapOf()

        return Json.decodeFromString<Map<Locale, String>>(s)
    }
}

object WordTypeConverter {
    @TypeConverter
    @JvmStatic
    fun fromType(value: String?): Type =
        value?.let { Type.from(it) } ?: Type.UNKNOWN

    @TypeConverter
    @JvmStatic
    fun toType(type: Type): String = type.typ
}

object ModalityConverter {
    @TypeConverter
    @JvmStatic
    fun fromModality(value: String?): Modality =
        value?.let { Modality.from(it) } ?: Modality.UNKNOWN

    @TypeConverter
    @JvmStatic
    fun toModality(modality: Modality): String = modality.mod
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
    @OptIn(ExperimentalTime::class)
    @TypeConverter
    @JvmStatic
    fun toDate(dateLong: Long?): LocalDate? {
        return dateLong?.let {
            Instant.fromEpochMilliseconds(it)
                .toLocalDateTime(TimeZone.UTC)
                .date
        }
    }

    @OptIn(ExperimentalTime::class)
    @TypeConverter
    @JvmStatic
    fun fromDate(date: LocalDate?): Long? {
        return date?.toEpochDays()
    }
}

class ListTypeConverter {
    @TypeConverter
    fun fromListType(value: WordList.ListType): String {
        return value.type
    }

    @TypeConverter
    fun toListType(value: String): WordList.ListType {
        return WordList.ListType.entries.first { it.type.equals(value, ignoreCase = true) }
    }
}