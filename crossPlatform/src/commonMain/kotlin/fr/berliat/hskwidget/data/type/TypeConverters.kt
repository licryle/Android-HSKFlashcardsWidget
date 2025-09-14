package fr.berliat.hskwidget.data.type

import androidx.room.TypeConverter
import fr.berliat.hskwidget.core.Locale
import fr.berliat.hskwidget.data.model.AnnotatedChineseWord
import fr.berliat.hskwidget.data.model.ChineseWord
import fr.berliat.hskwidget.data.model.ChineseWordAnnotation
import fr.berliat.hskwidget.data.model.WordList
import kotlinx.datetime.Instant

import kotlinx.serialization.json.Json

object DefinitionsConverter {
    @TypeConverter
    fun fromStringMap(value: Map<Locale, String>?): String? {
        return Json.encodeToString(value)
    }

    @TypeConverter
    fun fromString(s: String?): Map<Locale, String>? {
        if (s == null)
            return mapOf()

        val defs = Json.decodeFromString<Map<String, String>>(s)
        return defs.mapKeys { Locale.fromCode(it.key)!! }
    }
}

object WordTypeConverter {
    @TypeConverter
    fun fromType(value: String?): Type =
        value?.let { Type.from(it) } ?: Type.UNKNOWN

    @TypeConverter
    fun toType(type: Type): String = type.typ
}

object ModalityConverter {
    @TypeConverter
    fun fromModality(value: String?): Modality =
        value?.let { Modality.from(it) } ?: Modality.UNKNOWN

    @TypeConverter
    fun toModality(modality: Modality): String = modality.mod
}

object AnnotatedChineseWordsConverter {
    @TypeConverter
    fun fromMapToList(m: Map<ChineseWordAnnotation, List<ChineseWord>>): List<AnnotatedChineseWord> {
        val words = mutableSetOf<AnnotatedChineseWord>()

        m.forEach {
            words.add(AnnotatedChineseWord(it.value[0], it.key))
        }

        return words.toList()
    }

    @TypeConverter
    fun fromMapToFirst(m: Map<ChineseWordAnnotation, List<ChineseWord>>): AnnotatedChineseWord? {
        val words = fromMapToList(m)

        if (words.isEmpty())
            return null

        return words.first()
    }

    @TypeConverter
    fun fromListToMap(l: List<Map<ChineseWordAnnotation, List<ChineseWord>>>): Map<String, AnnotatedChineseWord> {
        val words = mutableMapOf<String, AnnotatedChineseWord>()

        l.forEach {
            words[it.keys.first().simplified] =
                AnnotatedChineseWord(it.values.first()[0], it.keys.first())
        }

        return words
    }
}

object InstantConverter {
    @TypeConverter
    fun toInstant(epochMillis: Long?): Instant? {
        return epochMillis?.let { Instant.fromEpochMilliseconds(epochMillis) }
    }

    @TypeConverter
    fun fromInstant(instant: Instant?): Long? {
        return instant?.toEpochMilliseconds()
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