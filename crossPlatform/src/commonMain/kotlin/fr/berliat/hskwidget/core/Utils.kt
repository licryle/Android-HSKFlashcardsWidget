package fr.berliat.hskwidget.core

import fr.berliat.hsktextviews.HSKTextSegmenter
import fr.berliat.hskwidget.BuildKonfig
import fr.berliat.hskwidget.data.dao.AnkiDAO
import fr.berliat.hskwidget.data.repo.ChineseWordFrequencyRepo
import fr.berliat.hskwidget.domain.SearchQuery

import kotlinx.coroutines.CoroutineScope

import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import kotlin.math.pow
import kotlin.math.round

object Utils {
    fun openLink(url: String) = ExpectedUtils.openLink(url)

    fun sendEmail(email: String, subject: String, body: String): Boolean =
        ExpectedUtils.sendEmail(email, subject, body)

    fun getAppVersion(): Int = BuildKonfig.VERSION_CODE

    fun getAnkiDAO(): AnkiDAO = ExpectedUtils.getAnkiDAO()

    fun getHSKSegmenter(): HSKTextSegmenter = ExpectedUtils.getHSKSegmenter()

    fun copyToClipBoard(s: String) = ExpectedUtils.copyToClipBoard(s)

    fun playWordInBackground(word: String) = ExpectedUtils.playWordInBackground(word)

    fun toast(stringRes: StringResource, args: List<String> = emptyList()) {
        CoroutineScope(AppDispatchers.IO).launch {
            toast(getString(stringRes, *args.toTypedArray()))
        }
    }

    fun toast(s: String) = ExpectedUtils.toast(s)

    fun openAppForSearchQuery(query: SearchQuery) = ExpectedUtils.openAppForSearchQuery(query)

    fun incrementConsultedWord(word: String) {
        HSKAppServices.appScope.launch(AppDispatchers.IO) {
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
}

expect object ExpectedUtils {
    fun openLink(url: String)
    fun sendEmail(email: String, subject: String = "", body: String = "") : Boolean

    fun getHSKSegmenter() : HSKTextSegmenter

    internal fun getAnkiDAO(): AnkiDAO


    fun copyToClipBoard(s: String)
    fun playWordInBackground(word: String)

    fun toast(s: String)

    fun openAppForSearchQuery(query: SearchQuery)
}

fun String.capitalize() =
    this.lowercase().replaceFirstChar { it.uppercaseChar() }

fun String.toSafeFileName(): String {
    // Keep letters, digits, underscore, dash, and dot
    return this.replace(Regex("[^A-Za-z0-9._-]"), "_")
}

fun Long.fromKBToMB(decimals: Int = 2): String {
    val mb = this.toDouble() / (1024 * 1024)
    val factor = 10.0.pow(decimals)
    val rounded = round(mb * factor) / factor
    return rounded.toString()
}

fun Instant.YYMMDD(timeZone: TimeZone = TimeZone.currentSystemDefault()): String {
    val local = this.toLocalDateTime(timeZone)
    val month = local.monthNumber.toString().padStart(2, '0')
    val day = local.dayOfMonth.toString().padStart(2, '0')
    return "${local.year}/$month/$day"
}

fun Instant.YYMMDDHHMMSS(timeZone: TimeZone = TimeZone.currentSystemDefault()): String {
    val local = this.toLocalDateTime(timeZone)
    val month = local.monthNumber.toString().padStart(2, '0')
    val day = local.dayOfMonth.toString().padStart(2, '0')

    val hour = local.hour.toString().padStart(2, '0')
    val minutes = local.minute.toString().padStart(2, '0')
    val seconds = local.second.toString().padStart(2, '0')

    return "${local.year}/$month/$day $hour:$minutes:$seconds"
}