package fr.berliat.hskwidget.core

import fr.berliat.hsktextviews.HSKTextSegmenter
import fr.berliat.hsktextviews.HSKTextSegmenterListener
import fr.berliat.hskwidget.data.dao.AnkiDAO
import fr.berliat.hskwidget.domain.SearchQuery

import kotlinx.coroutines.withContext
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents

import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVSpeechUtterance
import platform.AVFAudio.outputVolume
import platform.AVFAudio.*

import platform.Foundation.NSURL
import platform.Foundation.NSMakeRange
import platform.Foundation.NSNotFound
import platform.Foundation.NSRange

import platform.NaturalLanguage.NLTokenUnit
import platform.NaturalLanguage.*

import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString
import platform.UIKit.UIPasteboard

actual object ExpectedUtils {
	val TTSynthesizer = AVSpeechSynthesizer()

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

        override suspend fun preload() {
            withContext(AppDispatchers.Main) {
                listener?.onIsSegmenterReady()
            }
        }

        @OptIn(ExperimentalForeignApi::class)
        override fun segment(text: String): Array<String>? {
            val tokenizer = NLTokenizer(NLTokenUnit.NLTokenUnitWord)
            tokenizer.string = text

            val tokens = mutableListOf<String>()

            tokenizer.enumerateTokensInRange(NSMakeRange(0u, text.length.toULong())) { tokenRange, _, stop ->
                val swiftRange = tokenRange.useContents {
                    // Inside this block, 'this' refers to the actual NSRange struct,
                    // which has 'location' and 'length' properties.
                    this.toKmpIntRange(text)
                }

                if (swiftRange != null) {
                    tokens.add(text.substring(swiftRange))
                }
            }
            return tokens.toTypedArray()
        }
        override fun isReady(): Boolean { return true }
    }

    internal actual fun getAnkiDAO(): AnkiDAO {
        return AnkiDAO()
    }

    internal actual fun copyToClipBoard(s: String) {
        UIPasteboard.generalPasteboard.string = s
    }

	@OptIn(ExperimentalForeignApi::class)
	internal fun ensureTTSSetup() {
		val audioSession = AVAudioSession.sharedInstance()
		try {
			// Set category to .playback to bypass the silent switch
			audioSession.setCategory(
				AVAudioSessionCategoryPlayback,
				error = null
			)
			audioSession.setActive(true, error = null)
		} catch (e: Exception) {
			println("Audio session setup failed: $e")
		}

		// 2. Check for voices (Option 2)
		val availableVoices = AVSpeechSynthesisVoice.speechVoices() as List<AVSpeechSynthesisVoice>
		if (!availableVoices.any { it.language == "zh-CN" }) {
			println("Error: No voices installed. Cannot speak.")
			// ToDo add dialog
			openSettings()
		}
	}

    internal actual fun isMuted() : Boolean {
		val vol = AVAudioSession.sharedInstance().outputVolume
        return vol == 0.0F
    }

    internal actual fun playWordInBackground(word: String) {
		ensureTTSSetup()
        val utterance = AVSpeechUtterance.speechUtteranceWithString(string = word)

        utterance.voice = AVSpeechSynthesisVoice.voiceWithLanguage("zh-CN")

		TTSynthesizer.speakUtterance(utterance)
    }

    internal actual fun openAppForSearchQuery(query: SearchQuery) {

    }

	internal fun openSettings() {
		val settingsUrl = NSURL(string = UIApplicationOpenSettingsURLString )
		UIApplication.sharedApplication.openURL(settingsUrl)
	}
}

fun NSRange.toKmpIntRange(string: String): IntRange? {
    // 'this' here is an actual NSRange struct, so location/length works fine here
    if (this.location == NSNotFound.toULong()) return null
    val start = this.location.toInt()
    val end = (this.location + this.length).toInt()
    if (start < 0 || end > string.length || start > end) return null
    return IntRange(start, end - 1)
}
