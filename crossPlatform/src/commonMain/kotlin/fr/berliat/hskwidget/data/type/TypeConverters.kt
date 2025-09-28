package fr.berliat.hskwidget.data.type

import androidx.room.TypeConverter
import fr.berliat.hskwidget.core.Locale
import fr.berliat.hskwidget.core.LocaleSerializer
import fr.berliat.hskwidget.data.model.AnnotatedChineseWord
import fr.berliat.hskwidget.data.model.ChineseWord
import fr.berliat.hskwidget.data.model.ChineseWordAnnotation
import fr.berliat.hskwidget.data.model.WordList
import kotlinx.datetime.Instant
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer

import kotlinx.serialization.json.Json

object DefinitionsConverter {
    @TypeConverter
    fun fromStringMap(value: Map<Locale, String>?): String? {
        if (value == null) return null
        return Json.encodeToString(
            MapSerializer(LocaleSerializer, String.serializer()),
            value
        )
    }

    @TypeConverter
    fun fromString(s: String?): Map<Locale, String>? {
        if (s == null)
            return mapOf()

        return Json.decodeFromString(
            MapSerializer(LocaleSerializer, String.serializer()),
            s
        )
    }
}

object WordTypeConverter {
    @TypeConverter
    fun fromType(value: String?): WordType =
        value?.let { WordType.from(it) } ?: WordType.UNKNOWN

    @TypeConverter
    fun toType(wordType: WordType): String = wordType.wordType
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