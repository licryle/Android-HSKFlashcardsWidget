package fr.berliat.hskwidget.core

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences

import fr.berliat.hsktextviews.HSKTextSegmenter
import fr.berliat.hskwidget.data.dao.AnkiDAO
import fr.berliat.hskwidget.domain.SearchQuery

import kotlinx.cinterop.ExperimentalForeignApi

import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

import okio.Path.Companion.toPath

actual object ExpectedUtils {
    actual fun openLink(url: String) {
        val nsUrl = NSURL.URLWithString(url) ?: return
        UIApplication.sharedApplication.openURL(nsUrl)
    }

    actual fun sendEmail(email: String, subject: String, body: String) : Boolean {
        val urlString = "mailto:$email?subject=${subject}&body=${body}"
        openLink(urlString)
        return true
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

    const val DATABASE_FILENAME = "Mandarin_Assistant.db"

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

    actual fun logAnalyticsEvent(event: Utils.ANALYTICS_EVENTS, params: Map<String, String>) {
    }

    actual fun logAnalyticsError(module: String, error: String, details: String) {
    }

    actual fun getHSKSegmenter(): HSKTextSegmenter {
        TODO("Not yet implemented")
    }

    internal actual fun getAnkiDAO(): AnkiDAO {
        TODO("Not yet implemented")
    }

    actual fun copyToClipBoard(s: String) {
    }

    actual fun playWordInBackground(word: String) {
    }

    actual fun logAnalyticsWidgetAction(event: Utils.ANALYTICS_EVENTS, widgetId: Int) {
    }

    actual fun toast(s: String) {
    }

    actual fun openAppForSearchQuery(query: SearchQuery) {
    }
}