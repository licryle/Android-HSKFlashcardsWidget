package fr.berliat.hskwidget.ui

import fr.berliat.hskwidget.Res
import fr.berliat.hskwidget.data.type.*
import fr.berliat.hskwidget.enum_class_level_advanced_1
import fr.berliat.hskwidget.enum_class_level_advanced_2
import fr.berliat.hskwidget.enum_class_level_advanced_3
import fr.berliat.hskwidget.enum_class_level_elementary_1
import fr.berliat.hskwidget.enum_class_level_elementary_2
import fr.berliat.hskwidget.enum_class_level_elementary_3
import fr.berliat.hskwidget.enum_class_level_elementary_4
import fr.berliat.hskwidget.enum_class_level_intermediate_1
import fr.berliat.hskwidget.enum_class_level_intermediate_2
import fr.berliat.hskwidget.enum_class_level_intermediate_3
import fr.berliat.hskwidget.enum_class_type_fast_reading
import fr.berliat.hskwidget.enum_class_type_listening
import fr.berliat.hskwidget.enum_class_type_reading
import fr.berliat.hskwidget.enum_class_type_speaking
import fr.berliat.hskwidget.enum_class_type_writing
import fr.berliat.hskwidget.enum_modality_oral
import fr.berliat.hskwidget.enum_modality_oral_written
import fr.berliat.hskwidget.enum_modality_written
import fr.berliat.hskwidget.enum_word_type_adjective
import fr.berliat.hskwidget.enum_word_type_adverb
import fr.berliat.hskwidget.enum_word_type_conjunction
import fr.berliat.hskwidget.enum_word_type_idiom
import fr.berliat.hskwidget.enum_word_type_interjection
import fr.berliat.hskwidget.enum_word_type_noun
import fr.berliat.hskwidget.enum_word_type_preposition
import fr.berliat.hskwidget.enum_word_type_verb
import fr.berliat.hskwidget.generic_not_applicable
import fr.berliat.hskwidget.generic_other
import org.jetbrains.compose.resources.StringResource

fun ClassType.toRes(): StringResource = when (this) {
    ClassType.Speaking -> Res.string.enum_class_type_speaking
    ClassType.Writing -> Res.string.enum_class_type_writing
    ClassType.Reading -> Res.string.enum_class_type_reading
    ClassType.Listening -> Res.string.enum_class_type_listening
    ClassType.FastReading -> Res.string.enum_class_type_fast_reading
    ClassType.NotFromClass -> Res.string.generic_other
}

fun ClassLevel.toRes(): StringResource = when (this) {
    ClassLevel.Elementary1 -> Res.string.enum_class_level_elementary_1
    ClassLevel.Elementary2 -> Res.string.enum_class_level_elementary_2
    ClassLevel.Elementary3 -> Res.string.enum_class_level_elementary_3
    ClassLevel.Elementary4 -> Res.string.enum_class_level_elementary_4
    ClassLevel.Intermediate1 -> Res.string.enum_class_level_intermediate_1
    ClassLevel.Intermediate2 -> Res.string.enum_class_level_intermediate_2
    ClassLevel.Intermediate3 -> Res.string.enum_class_level_intermediate_3
    ClassLevel.Advanced1 -> Res.string.enum_class_level_advanced_1
    ClassLevel.Advanced2 -> Res.string.enum_class_level_advanced_2
    ClassLevel.Advanced3 -> Res.string.enum_class_level_advanced_3
    ClassLevel.NotFromClass -> Res.string.generic_other
}

fun WordType.toRes(): StringResource = when (this) {
    WordType.NOUN -> Res.string.enum_word_type_noun
    WordType.VERB -> Res.string.enum_word_type_verb
    WordType.ADJECTIVE -> Res.string.enum_word_type_adjective
    WordType.ADVERB -> Res.string.enum_word_type_adverb
    WordType.CONJUNCTION -> Res.string.enum_word_type_conjunction
    WordType.PREPOSITION -> Res.string.enum_word_type_preposition
    WordType.INTERJECTION -> Res.string.enum_word_type_interjection
    WordType.IDIOM -> Res.string.enum_word_type_idiom
    WordType.UNKNOWN -> Res.string.generic_not_applicable
}

fun Modality.toRes(): StringResource = when (this) {
    Modality.ORAL -> Res.string.enum_modality_oral
    Modality.WRITTEN -> Res.string.enum_modality_written
    Modality.ORAL_WRITTEN -> Res.string.enum_modality_oral_written
    Modality.UNKNOWN -> Res.string.generic_not_applicable
}
