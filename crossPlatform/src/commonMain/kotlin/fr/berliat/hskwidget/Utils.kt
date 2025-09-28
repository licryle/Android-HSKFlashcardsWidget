package fr.berliat.hskwidget

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences

import fr.berliat.hskwidget.domain.AnkiSyncWordListsService
import fr.berliat.hsktextviews.HSKTextSegmenter
import fr.berliat.hskwidget.core.HSKAppServices
import fr.berliat.hskwidget.data.dao.AnkiDAO
import fr.berliat.hskwidget.data.repo.ChineseWordFrequencyRepo
import fr.berliat.hskwidget.domain.SearchQuery

import io.github.vinceglb.filekit.BookmarkData
import io.github.vinceglb.filekit.PlatformFile

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

import org.jetbrains.compose.resources.StringResource
import kotlin.reflect.KClass

// KMP e
typealias KAnkiDelegator = suspend ((suspend () -> Result<Unit>)?) -> Unit
typealias KAnkiServiceDelegator = suspend (serviceClass: KClass<out AnkiSyncWordListsService>) -> Unit

object Utils {
    fun openLink(url: String) = ExpectedUtils.openLink(url)

    fun sendEmail(email: String, subject: String, body: String): Boolean =
        ExpectedUtils.sendEmail(email, subject, body)

    fun getPlatform(): String = ExpectedUtils.getPlatform()

    fun getAppVersion(): String = ExpectedUtils.getAppVersion()

    fun logAnalyticsScreenView(screen: String) = ExpectedUtils.logAnalyticsScreenView(screen)

    fun logAnalyticsEvent(event: ANALYTICS_EVENTS, params: Map<String, String> = emptyMap()) =
        ExpectedUtils.logAnalyticsEvent(event, params)

    fun logAnalyticsError(module: String, error: String, details: String) =
        ExpectedUtils.logAnalyticsError(module, error, details)

    fun getDataStore(file: String): DataStore<Preferences> =
        ExpectedUtils.getDataStore(file)

    fun getAnkiDAO(): AnkiDAO = ExpectedUtils.getAnkiDAO()

    fun getHSKSegmenter(): HSKTextSegmenter = ExpectedUtils.getHSKSegmenter()

    fun copyToClipBoard(s: String) = ExpectedUtils.copyToClipBoard(s)

    fun playWordInBackground(word: String) = ExpectedUtils.playWordInBackground(word)

    fun toast(stringRes: StringResource, args: List<String> = emptyList()) =
        ExpectedUtils.toast(stringRes, args)

    fun openAppForSearchQuery(query: SearchQuery) = ExpectedUtils.openAppForSearchQuery(query)

    fun incrementConsultedWord(word: String) {
        HSKAppServices.appScope.launch(Dispatchers.IO) {
            val db = HSKAppServices.database
            val frequencyWordsRepo = ChineseWordFrequencyRepo(
                db.chineseWordFrequencyDAO(),
                db.annotatedChineseWordDAO()
            )

            frequencyWordsRepo.incrementConsulted(word)
        }
    }

    fun getRandomString(length: Int): String {
        val allowedChars = ('A'..'Z') + ('a'..'z') + ('0'..'9')
        return (1..length)
            .map { allowedChars.random() }
            .joinToString("")
    }

    enum class ANALYTICS_EVENTS {
        SCREEN_VIEW,
        AUTO_WORD_CHANGE,
        ERROR, // Use logAnalyticsError for details
        WIDGET_PLAY_WORD,
        WIDGET_MANUAL_WORD_CHANGE,
        WIDGET_RECONFIGURE,
        WIDGET_CONFIG_VIEW,
        WIGDET_RESIZE,
        WIGDET_ADD, // Would be great to add
        WIDGET_EXPAND,
        WIDGET_COLLAPSE,
        WIGDET_REMOVE,
        WIDGET_OPEN_DICTIONARY,
        WIDGET_COPY_WORD,
        CONFIG_BACKUP_ON,
        CONFIG_BACKUP_OFF,
        CONFIG_BACKUP_RESTORE,
        CONFIG_BACKUPCLOUD_ON, // Reserved for future use
        CONFIG_BACKUPCLOUD_OFF, // Reserved for future use
        CONFIG_BACKUPCLOUD_RESTORE,
        CONFIG_BACKUPCLOUD_BACKUP,
        CONFIG_ANKI_SYNC_ON,
        CONFIG_ANKI_SYNC_OFF,
        ANNOTATION_SAVE,
        ANNOTATION_DELETE,
        LIST_CREATE,
        LIST_DELETE,
        LIST_MODIFY_WORD,
        LIST_RENAME,
        DICT_HSK3_ON,
        DICT_HSK3_OFF,
        DICT_ANNOTATION_ON,
        DICT_ANNOTATION_OFF,
        DICT_SEARCH,
        OCR_WORD_NOTFOUND,
        OCR_WORD_FOUND,
        PURCHASE_CLICK,
        PURCHASE_FAILED,
        PURCHASE_SUCCESS
    }
}

expect object ExpectedUtils {
    fun openLink(url: String)
    fun sendEmail(email: String, subject: String = "", body: String = "") : Boolean
    fun getPlatform(): String
    // commonMain
    fun getAppVersion(): String

    fun logAnalyticsScreenView(screen: String)
    fun logAnalyticsEvent(event: Utils.ANALYTICS_EVENTS,
                          params: Map<String, String> = mapOf())
    fun logAnalyticsError(module: String, error: String, details: String)

    fun getDataStore(file: String): DataStore<Preferences>

    fun getHSKSegmenter() : HSKTextSegmenter

    internal fun getAnkiDAO(): AnkiDAO


    fun copyToClipBoard(s: String)
    fun playWordInBackground(word: String)

    fun toast(stringRes: StringResource, args: List<String> = emptyList<String>())

    fun openAppForSearchQuery(query: SearchQuery)

    suspend fun copyFileSafely(sourceFile: PlatformFile, destinationDir: BookmarkData, filename: String)
}

expect fun PlatformFile.createdAt(): Instant?

expect fun PlatformFile.lastModified(): Instant

fun String.capitalize() =
    this.toString().lowercase().replaceFirstChar { it.uppercaseChar() }

fun Instant.YYMMDD(timeZone: TimeZone = TimeZone.currentSystemDefault()): String {
    val local = this.toLocalDateTime(timeZone)
    val month = local.monthNumber.toString().padStart(2, '0')
    val day = local.dayOfMonth.toString().padStart(2, '0')
    return "${local.year}-$month-$day"
}

fun Instant.YYMMDDHHMMSS(timeZone: TimeZone = TimeZone.currentSystemDefault()): String {
    val local = this.toLocalDateTime(timeZone)
    val month = local.monthNumber.toString().padStart(2, '0')
    val day = local.dayOfMonth.toString().padStart(2, '0')

    val hour = local.hour.toString().padStart(2, '0')
    val minutes = local.minute.toString().padStart(2, '0')
    val seconds = local.second.toString().padStart(2, '0')

    return "${local.year}-$month-$day-$hour-$minutes-$seconds"
}