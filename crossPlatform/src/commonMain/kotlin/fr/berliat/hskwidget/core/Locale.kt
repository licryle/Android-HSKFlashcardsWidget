package fr.berliat.hskwidget.core

enum class Locale(val code: String) {
    ENGLISH("en"),
    CHINESE("zh"),
    FRENCH("fr"),
    SPANISH("es"),
    CN_HSK3("zh_CN_HSK03"),
    SIMPLIFIED_CHINESE("zh_CN");

    companion object {
        // Get enum from string code
        fun fromCode(code: String): Locale? =
            entries.firstOrNull { it.code == code }

        fun getDefault(): Locale {
            return ENGLISH
        }
    }
}