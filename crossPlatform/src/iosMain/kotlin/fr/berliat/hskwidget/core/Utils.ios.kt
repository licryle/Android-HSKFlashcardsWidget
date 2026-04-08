package fr.berliat.hskwidget.core

import fr.berliat.hsktextviews.HSKTextSegmenter
import fr.berliat.hsktextviews.HSKTextSegmenterListener
import fr.berliat.hskwidget.data.dao.AnkiDAO
import fr.berliat.hskwidget.domain.SearchQuery
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.databasesDir
import io.github.vinceglb.filekit.filesDir

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
import platform.Foundation.NSFileManager

import platform.NaturalLanguage.NLTokenUnit
import platform.NaturalLanguage.*

import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString
import platform.UIKit.UIPasteboard

actual object ExpectedUtils {
	val TTSynthesizer = AVSpeechSynthesizer()

    internal actual fun getAppDataPath(): PlatformFile {
        val path = NSFileManager.defaultManager
            .containerURLForSecurityApplicationGroupIdentifier("group.fr.berliat.hskwidget")?.path
        
        return path?.let { PlatformFile(it) } ?: FileKit.filesDir
    }

    internal actual fun getAppDatabasePath(): PlatformFile {
        val path = NSFileManager.defaultManager
            .containerURLForSecurityApplicationGroupIdentifier("group.fr.berliat.hskwidget")?.path

        return path?.let { PlatformFile(it) } ?: FileKit.databasesDir
    }

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

            tokenizer.enumerateTokensInRange(NSMakeRange(0u, text.length.toULong())) { tokenRange, _, _ ->
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
			// Set category to .playback with options to allow background mixing.
            // These options are critical for Widget/AppIntent audio to trigger.
			audioSession.setCategory(
				AVAudioSessionCategoryPlayback,
				AVAudioSessionCategoryOptionDuckOthers or AVAudioSessionCategoryOptionMixWithOthers,
				error = null
			)
			audioSession.setActive(true, error = null)
		} catch (e: Exception) {
			println("Audio session setup failed: $e")
		}

		// 2. Check for voices (Option 2)
        val voices = AVSpeechSynthesisVoice.speechVoices() as List<AVSpeechSynthesisVoice>
        val voice = AVSpeechSynthesisVoice.voiceWithLanguage("zh-CN")
            ?: AVSpeechSynthesisVoice.voiceWithLanguage("zh-Hans")
            ?: voices.firstOrNull { it.language.startsWith("zh") }

		if (voice == null) {
			println("Error: No voices installed. Cannot speak.")
			// ToDo add dialog
			openSettings()
		}
	}

	@OptIn(ExperimentalForeignApi::class)
    internal actual fun isMuted() : Boolean {
		val audioSession = AVAudioSession.sharedInstance()
		if (audioSession.outputVolume > 0f) return false

		// In background contexts like AppIntents/Widgets, outputVolume often returns 0
		// until the session is active. We try to activate it briefly to get a real reading.
		try {
			audioSession.setCategory(AVAudioSessionCategoryPlayback, error = null)
			audioSession.setActive(true, error = null)
		} catch (_: Exception) {
            return false
        }

		return audioSession.outputVolume == 0.0f
    }

    internal actual fun playWordInBackground(word: String) {
        if (word.isBlank()) return
		ensureTTSSetup()

        // Interrupt any current speech to ensure the new word is played immediately
        if (TTSynthesizer.isSpeaking()) {
            println("TTS: Synthesizer already speaking, stopping current utterance.")
            TTSynthesizer.stopSpeakingAtBoundary(AVSpeechBoundary.AVSpeechBoundaryImmediate)
        }

        val utterance = AVSpeechUtterance(string = word)
        println("TTS: Utterance created successfully: $utterance")
        
        // Find a Chinese voice with fallbacks
        val voice = AVSpeechSynthesisVoice.voiceWithLanguage("zh-CN")
            ?: AVSpeechSynthesisVoice.voiceWithLanguage("zh-Hans")
            ?: AVSpeechSynthesisVoice.speechVoices().mapNotNull { it as? AVSpeechSynthesisVoice }.firstOrNull { 
                it.language.startsWith("zh") 
            }

        if (voice != null) {
            println("TTS: Selected voice: ${voice.language} (${voice.name})")
            utterance.voice = voice
        } else {
            println("TTS: WARNING - No Chinese voice found. Using default.")
        }

        utterance.rate = AVSpeechUtteranceDefaultSpeechRate
        utterance.volume = 1.0f

        try {
            println("TTS: Calling speakUtterance...")
		    TTSynthesizer.speakUtterance(utterance)
            println("TTS: speakUtterance called.")
        } catch (e: Exception) {
            println("TTS: CRITICAL ERROR - speakUtterance failed: $e")
        }
    }

    internal actual fun openAppForSearchQuery(query: SearchQuery) {
        // Todo: implement
    }

	internal fun openSettings() {
		val settingsUrl = NSURL(string = UIApplicationOpenSettingsURLString )
		UIApplication.sharedApplication.openURL(settingsUrl)
	}

    internal actual fun attemptAddDesktopWidget(): Boolean {
        return false
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
