package fr.berliat.hskwidget.core

import platform.Foundation.NSUserDefaults

actual object LocaleManager {
    actual fun setLanguage(languageCode: String?) {
        if (languageCode == null) {
            NSUserDefaults.standardUserDefaults.removeObjectForKey("AppleLanguages")
        } else {
            NSUserDefaults.standardUserDefaults.setObject(listOf(languageCode), forKey = "AppleLanguages")
        }
    }

    actual fun getCurrentLanguage(): String? {
        val languages = NSUserDefaults.standardUserDefaults.objectForKey("AppleLanguages") as? List<*>
        return languages?.firstOrNull() as? String
    }
}