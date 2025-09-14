package fr.berliat.hskwidget.data.type

enum class WordType(val wordType: String) {
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
        fun from(findValue: String): WordType {
            return try {
                WordType.valueOf(findValue)
            } catch (_: Exception) {
                UNKNOWN
            }
        }
    }
}