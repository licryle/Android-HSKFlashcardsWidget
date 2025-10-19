package fr.berliat.hskwidget.core

import fr.berliat.hsktextviews.HSKTextSegmenter
import fr.berliat.hsktextviews.HSKTextSegmenterListener
import fr.berliat.hskwidget.data.dao.AnkiDAO
import fr.berliat.hskwidget.domain.SearchQuery

import platform.Foundation.NSURL
import platform.UIKit.UIApplication

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

    actual fun logAnalyticsEvent(event: Utils.ANALYTICS_EVENTS, params: Map<String, String>) {
    }

    actual fun logAnalyticsWidgetAction(event: Utils.ANALYTICS_EVENTS, widgetId: Int) {
    }

    actual fun getHSKSegmenter(): HSKTextSegmenter = object : HSKTextSegmenter {
		override var listener: HSKTextSegmenterListener? = null
		override suspend fun preload() {}
		override fun segment(text: String): Array<String>? { return null }
		override fun isReady(): Boolean { return false }
	}

    internal actual fun getAnkiDAO(): AnkiDAO {
        return AnkiDAO()
    }

    actual fun copyToClipBoard(s: String) {
    }

    actual fun playWordInBackground(word: String) {
    }

    actual fun toast(s: String) {
    }

    actual fun openAppForSearchQuery(query: SearchQuery) {
    }
}
