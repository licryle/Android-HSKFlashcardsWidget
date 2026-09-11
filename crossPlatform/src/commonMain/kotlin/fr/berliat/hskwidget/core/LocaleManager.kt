package fr.berliat.hskwidget.core

object LocaleManager {
    val supportedLocales = mapOf(
        "en" to "English",
        "fr" to "Français",
        "zh-Hans" to "简体中文"
    )

    fun setLocale(languageCode: String?) {
        PlatformLocaleManager.setLocale(languageCode)
    }

    fun getCurrentLocale(): String {
        val platformLocale = PlatformLocaleManager.getCurrentLocale()
        return if (supportedLocales.containsKey(platformLocale)) {
            platformLocale!!
        } else {
            supportedLocales.keys.first()
        }
    }
}

internal expect object PlatformLocaleManager {
    fun setLocale(languageCode: String?)
    fun getCurrentLocale(): String?
}
