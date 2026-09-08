package fr.berliat.hskwidget.core

import platform.Foundation.NSUserDefaults

actual object LocaleManager {
    actual fun setLanguage(languageCode: String?) {
        // iOS language selection is typically handled by the system or via "AppleLanguages" key in NSUserDefaults.
        // For a full implementation, one would need to restart the app or use a more complex localized bundle approach.
        // For now, we leave it as a placeholder as per the plan.
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