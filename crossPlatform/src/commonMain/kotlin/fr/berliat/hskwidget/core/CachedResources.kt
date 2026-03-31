package fr.berliat.hskwidget.core

import fr.berliat.hskwidget.*
import org.jetbrains.compose.resources.getString

object CachedResources {
    // Fallback values for static access before init
    var appName: String = "Mandarin Assistant"
        private set
    var appSlogan: String = "Study at every phone unlock"
        private set
    var widgetNextWord: String = "Next Word"
        private set
    var widgetSpeakWord: String = "Speak Word"
        private set
    var widgetNotConfigured: String = "No word.\nConfigure list(s)"
        private set
    var placeholderWord: String = "你好"
        private set
    var placeholderPinyin: String = "nǐ hǎo"
        private set
    var placeholderDefinition: String = "Hello"
        private set
    var placeholderLevel: String = "HSK 1"
        private set
    var placeholderLanguage: String = "en"
        private set

    suspend fun load(): CachedResources = CachedResources.apply {
        appName = getString(Res.string.app_name)
        appSlogan = getString(Res.string.app_slogan)
        widgetNextWord = getString(Res.string.dictionary_item_reload)
        widgetSpeakWord = getString(Res.string.widget_btn_speak)
        widgetNotConfigured = getString(Res.string.widget_not_configured)
        placeholderWord = getString(Res.string.widget_placeholder_word)
        placeholderPinyin = getString(Res.string.widget_placeholder_pinyin)
        placeholderDefinition = getString(Res.string.widget_placeholder_definition)
        placeholderLevel = getString(Res.string.widget_placeholder_level)
        placeholderLanguage = getString(Res.string.widget_placeholder_language)
    }
}
