package fr.berliat.hskwidget

import platform.UIKit.UIDevice
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.Foundation.NSBundle

actual object Utils {
    actual fun openLink(url: String) {
        val nsUrl = NSURL.URLWithString(url) ?: return
        UIApplication.sharedApplication.openURL(nsUrl)
    }

    actual fun sendEmail(email: String, subject: String, body: String) : Boolean {
        val urlString = "mailto:$email?subject=${subject}&body=${body}"
        openLink(urlString)
        return true
    }

    actual fun getPlatform(): String {
        return UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
    }

    actual fun getAppVersion(): String {
        val version = NSBundle.mainBundle.infoDictionary?.get("CFBundleShortVersionString") as? String
        val build = NSBundle.mainBundle.infoDictionary?.get("CFBundleVersion") as? String
        return if (version != null && build != null) "$version ($build)" else "1.0"
    }
    
    actual fun logAnalyticsScreenView(screen: String) {
    }
}