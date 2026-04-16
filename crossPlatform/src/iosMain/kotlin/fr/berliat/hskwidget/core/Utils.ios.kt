package fr.berliat.hskwidget.core

import fr.berliat.hsktextviews.HSKTextSegmenter
import fr.berliat.hsktextviews.HSKTextSegmenterListener
import fr.berliat.hskwidget.data.dao.AnkiDAO
import fr.berliat.hskwidget.domain.SearchQuery
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.databasesDir
import io.github.vinceglb.filekit.filesDir

import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents

import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVSpeechUtterance
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

@OptIn(ExperimentalForeignApi::class)
actual object ExpectedUtils {
	private val TTSynthesizer = AVSpeechSynthesizer()
    private var isAudioSessionSetup = false

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

        override fun segment(text: String): Array<String>? {
            val tokenizer = NLTokenizer(NLTokenUnit.NLTokenUnitWord)
            tokenizer.string = text

            val tokens = mutableListOf<String>()

            tokenizer.enumerateTokensInRange(NSMakeRange(0u, text.length.toULong())) { tokenRange, _, _ ->
                val swiftRange = tokenRange.useContents {
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

    internal actual fun isMuted() : Boolean {
        // Optimistic on iOS: outputVolume can be unreliable or blocking in background/launch.
        // Returning false allows playWordInBackground to trigger setup and play.
        return false
    }

    internal actual fun playWordInBackground(word: String) {
        if (word.isBlank()) return
        
        // Use appScope and Default dispatcher to perform setup without blocking UI
        HSKAppServices.appScope.launch(Dispatchers.Default) {
            try {
                if (!isAudioSessionSetup) {
                    val audioSession = AVAudioSession.sharedInstance()
                    audioSession.setCategory(
                        AVAudioSessionCategoryPlayback,
                        AVAudioSessionCategoryOptionDuckOthers or AVAudioSessionCategoryOptionMixWithOthers,
                        error = null
                    )
                    audioSession.setActive(true, error = null)
                    isAudioSessionSetup = true
                    // Small delay to let the session stabilize
                    delay(300)
                }

                // Find a Chinese voice
                val voice = AVSpeechSynthesisVoice.voiceWithLanguage("zh-CN")
                    ?: AVSpeechSynthesisVoice.voiceWithLanguage("zh-Hans")
                    ?: AVSpeechSynthesisVoice.speechVoices().mapNotNull { it as? AVSpeechSynthesisVoice }.firstOrNull { 
                        it.language.startsWith("zh") 
                    }

                val utterance = AVSpeechUtterance(string = word)
                if (voice != null) {
                    utterance.voice = voice
                }
                utterance.rate = AVSpeechUtteranceDefaultSpeechRate
                utterance.volume = 1.0f

                // Speech must be triggered on Main thread
                withContext(Dispatchers.Main) {
                    if (TTSynthesizer.isSpeaking()) {
                        TTSynthesizer.stopSpeakingAtBoundary(AVSpeechBoundary.AVSpeechBoundaryImmediate)
                        delay(200)
                    }

                    // Safety delay before speaking
                    delay(200)
                    TTSynthesizer.speakUtterance(utterance)
                }
            } catch (e: Exception) {
                println("TTS: playWordInBackground failed: ${e.message}")
            }
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

@OptIn(ExperimentalForeignApi::class)
fun NSRange.toKmpIntRange(string: String): IntRange? {
    if (this.location == NSNotFound.toULong()) return null
    val start = this.location.toInt()
    val end = (this.location + this.length).toInt()
    if (start < 0 || end > string.length || start > end) return null
    return IntRange(start, end - 1)
}
