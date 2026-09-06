package fr.berliat.hskwidget.core

import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.div
import io.github.vinceglb.filekit.toKotlinxIoPath
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.io.buffered
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readString
import kotlinx.io.writeString

object Logging {
    private var logFile: PlatformFile? = null

    class FileLogWriter(private val file: PlatformFile) : LogWriter() {
        override fun log(severity: Severity, message: String, tag: String, throwable: Throwable?) {
            val path = file.toKotlinxIoPath()
            try {
                // SystemFileSystem.sink with append=true is supported in newer kotlinx-io
                // If not, we might need a workaround, but let's try this first.
                SystemFileSystem.sink(path, append = true).buffered().use { sink ->
                    sink.writeString("[$severity] $tag: $message\n")
                    throwable?.let {
                        sink.writeString(it.stackTraceToString() + "\n")
                    }
                }
            } catch (e: Exception) {
                // Ignore to avoid infinite loop
            }
        }
    }

    fun setupFileLogging() {
        try {
            val file = Utils.getAppDataPath() / "app_logs.txt"
            logFile = file
            val path = file.toKotlinxIoPath()

            // Truncate the file on launch
            SystemFileSystem.sink(path, append = false).buffered().use { sink ->
                sink.writeString("--- App Launch ---\n")
            }

            Logger.addLogWriter(FileLogWriter(file))
        } catch (e: Exception) {
            Logger.e(tag = "Logging", messageString = "Failed to setup file logging", throwable = e)
        }
    }

    fun getLogFileContent(): String {
        val file = logFile ?: return ""
        val path = file.toKotlinxIoPath()
        return try {
            if (SystemFileSystem.exists(path)) {
                SystemFileSystem.source(path).buffered().use { it.readString() }
            } else {
                ""
            }
        } catch (e: Exception) {
            "Error reading logs: ${e.message}"
        }
    }

    val GlobalCoroutineExceptionHandler: CoroutineExceptionHandler =
        CoroutineExceptionHandler { context, exception ->
            Logger.e(
                tag = "CoroutineCrash",
                messageString = "Unhandled coroutine exception on context: $context",
                throwable = exception,
            )

            try {
                ExpectedLogging.logCrashalytics(exception)
            } catch (e: Throwable) {
                Logger.w(tag = "Logging", messageString = "Failed to log exception to Crashlytics: ${e.message}")
            }
        }

    fun logAnalyticsScreenView(screenName: String) {
        logAnalyticsEvent(
            ANALYTICS_EVENTS.SCREEN_VIEW,
            mapOf("SCREEN_NAME" to screenName)
        )
    }

    fun logAnalyticsEvent(event: ANALYTICS_EVENTS, params: Map<String, String> = emptyMap()) =
        ExpectedLogging.logAnalyticsEvent(event, params)

    fun logAnalyticsError(module: String, error: String, details: String) {
        logAnalyticsEvent(
            ANALYTICS_EVENTS.ERROR,
            mapOf(
                "MODULE" to module,
                "ERROR_ID" to error,
                "DETAILS" to details
            )
        )
    }

    fun logAnalyticsWidgetAction(event: ANALYTICS_EVENTS, widgetId: Int) =
        ExpectedLogging.logAnalyticsWidgetAction(event, widgetId)

    enum class ANALYTICS_EVENTS {
        SCREEN_VIEW,
        AUTO_WORD_CHANGE,
        ERROR, // Use logAnalyticsError for details
        WIDGET_PLAY_WORD,
        WIDGET_MANUAL_WORD_CHANGE,
        WIDGET_RECONFIGURE,
        WIDGET_CONFIG_VIEW,
        WIGDET_RESIZE,
        WIGDET_ADD,
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
        DICT_TEXT_SIZE_CHANGE,
        OCR_TEXT_SIZE_CHANGE,
        DICT_SEARCH,
        OCR_CAPTURE,
        OCR_WORD_NOTFOUND,
        OCR_WORD_FOUND,
        PURCHASE_CLICK,
        PURCHASE_FAILED,
        PURCHASE_SUCCESS
    }
}

expect object ExpectedLogging {
    internal fun logCrashalytics(e: Throwable)
    internal fun logAnalyticsEvent(event: Logging.ANALYTICS_EVENTS,
                          params: Map<String, String> = mapOf())
    internal fun logAnalyticsWidgetAction(event: Logging.ANALYTICS_EVENTS, widgetId: Int)
}
