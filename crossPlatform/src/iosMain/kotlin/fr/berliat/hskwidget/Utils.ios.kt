package fr.berliat.hskwidget

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.room.Room
import fr.berliat.hskwidget.data.store.ChineseWordsDatabase
import kotlinx.cinterop.ExperimentalForeignApi
import platform.UIKit.UIDevice
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.Foundation.NSBundle
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

import okio.Path.Companion.toPath

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

    @OptIn(ExperimentalForeignApi::class)
    private fun documentDirectory(): String {
        val documentDirectory = NSFileManager.defaultManager.URLForDirectory(
            directory = NSDocumentDirectory,
            inDomain = NSUserDomainMask,
            appropriateForURL = null,
            create = false,
            error = null,
        )
        return requireNotNull(documentDirectory?.path)
    }

    actual suspend fun getDatabaseInstance(): ChineseWordsDatabase {
        val dbFilePath = getDatabasePath()
        return Room.databaseBuilder<ChineseWordsDatabase>(
            name = dbFilePath,
        ).build()
    }

    actual fun getDatabasePath(): String {
        return documentDirectory() + "/$DATABASE_FILENAME"
    }

    const val DATABASE_FILENAME = "Mandarin_Assistant.db"
    actual fun logAnalyticsEvent(event: ANALYTICS_EVENTS) {
    }

    @OptIn(ExperimentalForeignApi::class)
    actual fun getDataStore(file: String): DataStore<Preferences> {
        val documentDirectory: NSURL? = NSFileManager.defaultManager.URLForDirectory(
            directory = NSDocumentDirectory,
            inDomain = NSUserDomainMask,
            appropriateForURL = null,
            create = false,
            error = null,
        )
        val dbPath = requireNotNull(documentDirectory).path + file

        return PreferenceDataStoreFactory.createWithPath(
            produceFile = { dbPath.toPath() }
        )
    }
}