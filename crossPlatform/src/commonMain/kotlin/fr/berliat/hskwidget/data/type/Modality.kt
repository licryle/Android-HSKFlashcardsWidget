package fr.berliat.hskwidget.data.type

enum class Modality(val mod: String) {
    ORAL("ORAL"),
    WRITTEN("WRITTEN"),
    ORAL_WRITTEN("ORAL_WRITTEN"),
    UNKNOWN("N/A");

    companion object {
        fun from(findValue: String): Modality {
            return try {
                Modality.valueOf(findValue)
            } catch (_: Exception) {
                UNKNOWN
            }
        }
    }
}