package fr.berliat.hskwidget.data.type

enum class Modality(val mod: String) {
    ORAL("ORAL"),
    WRITTEN("WRITTEN"),
    ORAL_WRITTEN("ORAL_WRITTEN"),
    UNKNOWN("N/A");

    override fun toString(): String {
        return super.toString().replace("_", " & ")
    }

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