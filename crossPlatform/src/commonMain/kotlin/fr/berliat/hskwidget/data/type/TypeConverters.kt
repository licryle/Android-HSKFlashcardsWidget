package fr.berliat.hskwidget.data.type

import androidx.room3.ColumnTypeConverter
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
    @ColumnTypeConverter
    fun fromStringMap(value: Map<Locale, String>?): String? {
        if (value == null) return null
        return Json.encodeToString(
            MapSerializer(LocaleSerializer, String.serializer()),
            value
        )
    }

    @ColumnTypeConverter
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
    @ColumnTypeConverter
    fun fromType(value: String?): WordType =
        value?.let { WordType.from(it) } ?: WordType.UNKNOWN

    @ColumnTypeConverter
    fun toType(wordType: WordType): String = wordType.wordType
}

object ModalityConverter {
    @ColumnTypeConverter
    fun fromModality(value: String?): Modality =
        value?.let { Modality.from(it) } ?: Modality.UNKNOWN

    @ColumnTypeConverter
    fun toModality(modality: Modality): String = modality.mod
}

object AnnotatedChineseWordsConverter {
    @ColumnTypeConverter
    fun fromMapToList(m: Map<ChineseWordAnnotation, List<ChineseWord>>): List<AnnotatedChineseWord> {
        val words = mutableSetOf<AnnotatedChineseWord>()

        m.forEach {
            words.add(AnnotatedChineseWord(it.value[0], it.key))
        }

        return words.toList()
    }

    @ColumnTypeConverter
    fun fromMapToFirst(m: Map<ChineseWordAnnotation, List<ChineseWord>>): AnnotatedChineseWord? {
        val words = fromMapToList(m)

        if (words.isEmpty())
            return null

        return words.first()
    }

    @ColumnTypeConverter
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
    @ColumnTypeConverter
    fun toInstant(epochMillis: Long?): Instant? {
        return epochMillis?.let { Instant.fromEpochMilliseconds(epochMillis) }
    }

    @ColumnTypeConverter
    fun fromInstant(instant: Instant?): Long? {
        return instant?.toEpochMilliseconds()
    }
}

class ListTypeConverter {
    @ColumnTypeConverter
    fun fromListType(value: WordList.ListType): String {
        return value.type
    }

    @ColumnTypeConverter
    fun toListType(value: String): WordList.ListType {
        return WordList.ListType.entries.first { it.type.equals(value, ignoreCase = true) }
    }
}