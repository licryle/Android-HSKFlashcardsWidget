package fr.berliat.hskwidget.data.type

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
            } catch (_: Exception) {
                UNKNOWN
            }
        }
    }
}