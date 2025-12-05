package fr.berliat.hskwidget.core

import fr.berliat.hsktextviews.HSKTextSegmenter
import fr.berliat.hsktextviews.HSKTextSegmenterListener
import fr.berliat.hskwidget.data.dao.AnkiDAO
import fr.berliat.hskwidget.domain.SearchQuery

import platform.Foundation.NSURL
import platform.UIKit.UIApplication

actual object ExpectedUtils {
    internal actual fun openLink(url: String) {
        val nsUrl = NSURL.URLWithString(url) ?: return
        UIApplication.sharedApplication.openURL(nsUrl)
    }

    internal actual fun sendEmail(email: String, subject: String, body: String) : Boolean {
        val urlString = "mailto:$email?subject=${subject}&body=${body}"
        openLink(urlString)
        return true
    }

    internal actual fun getHSKSegmenter(): HSKTextSegmenter = object : HSKTextSegmenter {
		override var listener: HSKTextSegmenterListener? = null
		override suspend fun preload() {}
		override fun segment(text: String): Array<String>? { return null }
		override fun isReady(): Boolean { return false }
	}

    internal actual fun getAnkiDAO(): AnkiDAO {
        return AnkiDAO()
    }

    internal actual fun copyToClipBoard(s: String) {
    }

    internal actual fun isMuted() : Boolean {

    }

    internal actual fun playWordInBackground(word: String) {
    }

    internal actual fun toast(s: String) {
    }

    internal actual fun openAppForSearchQuery(query: SearchQuery) {
    }
}
