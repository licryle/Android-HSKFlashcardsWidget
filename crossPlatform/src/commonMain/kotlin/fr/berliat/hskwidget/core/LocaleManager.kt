package fr.berliat.hskwidget.core

expect object LocaleManager {
    fun setLanguage(languageCode: String?)
    fun getCurrentLanguage(): String?
}