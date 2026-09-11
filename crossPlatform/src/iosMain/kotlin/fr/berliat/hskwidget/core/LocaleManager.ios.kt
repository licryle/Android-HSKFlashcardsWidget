package fr.berliat.hskwidget.core

import platform.Foundation.NSUserDefaults

internal actual object PlatformLocaleManager {
    actual fun setLocale(languageCode: String?) {
        if (languageCode == null) {
            NSUserDefaults.standardUserDefaults.removeObjectForKey("AppleLanguages")
        } else {
            NSUserDefaults.standardUserDefaults.setObject(listOf(languageCode), forKey = "AppleLanguages")
        }
    }

    actual fun getCurrentLocale(): String? {
        val languages = NSUserDefaults.standardUserDefaults.objectForKey("AppleLanguages") as? List<*>
        return languages?.firstOrNull() as? String
    }
}