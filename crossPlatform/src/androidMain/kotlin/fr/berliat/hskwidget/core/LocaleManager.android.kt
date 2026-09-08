package fr.berliat.hskwidget.core

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

actual object LocaleManager {
    actual fun setLanguage(languageCode: String?) {
        val appLocale: LocaleListCompat = if (languageCode == null) {
            LocaleListCompat.getEmptyLocaleList()
        } else {
            LocaleListCompat.forLanguageTags(languageCode)
        }
        AppCompatDelegate.setApplicationLocales(appLocale)
    }

    actual fun getCurrentLanguage(): String? {
        val locales = AppCompatDelegate.getApplicationLocales()
        return if (locales.isEmpty) null else locales.get(0)?.toLanguageTag()
    }
}