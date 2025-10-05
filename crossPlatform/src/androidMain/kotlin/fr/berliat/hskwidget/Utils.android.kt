package fr.berliat.hskwidget

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.widget.Toast

import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.Observer
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf

import co.touchlab.kermit.Logger

import fr.berliat.hsktextviews.HSKTextSegmenter
import fr.berliat.hskwidget.Utils.incrementConsultedWord
import fr.berliat.hskwidget.Utils.toast
import fr.berliat.hskwidget.core.BackgroundSpeechService
import fr.berliat.hskwidget.core.JiebaHSKTextSegmenter
import fr.berliat.hskwidget.crossPlatform.BuildKonfig
import fr.berliat.hskwidget.data.dao.AnkiDAO
import fr.berliat.hskwidget.domain.SearchQuery

import hskflashcardswidget.crossplatform.generated.resources.Res
import hskflashcardswidget.crossplatform.generated.resources.cancel
import hskflashcardswidget.crossplatform.generated.resources.copied_to_clipboard
import hskflashcardswidget.crossplatform.generated.resources.dialog_tts_error
import hskflashcardswidget.crossplatform.generated.resources.fix_it
import hskflashcardswidget.crossplatform.generated.resources.speech_failure_toast_chinese_unsupported
import hskflashcardswidget.crossplatform.generated.resources.speech_failure_toast_init
import hskflashcardswidget.crossplatform.generated.resources.speech_failure_toast_muted
import hskflashcardswidget.crossplatform.generated.resources.speech_failure_toast_unknown

import io.github.vinceglb.filekit.AndroidFile
import io.github.vinceglb.filekit.BookmarkData
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.context
import io.github.vinceglb.filekit.fromBookmarkData
import io.github.vinceglb.filekit.path

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant

import okio.Path.Companion.toPath

import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.attribute.BasicFileAttributes

@SuppressLint("StaticFieldLeak") // Only Storing Application context. No memory leak.
actual object ExpectedUtils {

    private var _context: Context? = null
    val context
        get() = _context!!

    private var _activityProvider: (() -> Activity)? = null
    val activity
        get() = _activityProvider!!.invoke()

    // Initialize once from Compose or Activity
    fun init(activity: Activity) {
        _context = activity.applicationContext
        _activityProvider = { activity }
    }

    fun requestPermissionNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context.applicationContext,
                    Manifest.permission.POST_NOTIFICATIONS
                )
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    activity,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    0
                )
            }
        }
    }

    actual fun openLink(url: String) {
        val intent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse(url)
        ).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        context.startActivity(intent)
    }

    actual fun sendEmail(email: String, subject: String, body: String) : Boolean {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:") // use only "mailto:", don't put address here
            putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
        }

        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
        } else {
            try {
                context.startActivity(Intent.createChooser(intent, "Send email with..."))
            } catch (_: Exception) {
                return false
            }
        }

        return true
    }

    actual fun getPlatform(): String {
        return "Android ${getAppVersion()}"
    }

    // androidMain
    actual fun getAppVersion(): Int = BuildKonfig.VERSION_CODE

    actual fun logAnalyticsScreenView(screen: String) {
    }

    actual fun logAnalyticsEvent(event: Utils.ANALYTICS_EVENTS,
                                 params: Map<String, String>) {
    }

    actual fun logAnalyticsWidgetAction(event: Utils.ANALYTICS_EVENTS, widgetId: Int) {
    }

    actual fun logAnalyticsError(module: String, error: String, details: String) {
        logAnalyticsEvent(
            Utils.ANALYTICS_EVENTS.ERROR,
            mapOf(
                "MODULE" to module,
                "ERROR_ID" to error,
                "DETAILS" to details
            )
        )
    }

    actual fun getDataStore(file: String): DataStore<Preferences> {
        return PreferenceDataStoreFactory.createWithPath(
            produceFile = { context.filesDir.resolve(file).absolutePath.toPath() }
        )
    }

    actual fun getAnkiDAO(): AnkiDAO {
        return AnkiDAO(context)
    }

    actual fun getHSKSegmenter() : HSKTextSegmenter {
        return JiebaHSKTextSegmenter()
    }

    actual fun copyToClipBoard(s: String) {
        // https://stackoverflow.com/a/28780585/3059536
        val context = context

        val clipboard =
            context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Copied Text", s)
        clipboard.setPrimaryClip(clip)

        toast(Res.string.copied_to_clipboard, listOf(s))

        logAnalyticsEvent(Utils.ANALYTICS_EVENTS.WIDGET_COPY_WORD)

        incrementConsultedWord(s)
    }

    actual fun playWordInBackground(word: String) {
        val context = context
        val speechRequest = OneTimeWorkRequestBuilder<BackgroundSpeechService>()
            .setInputData(workDataOf(Pair("word", word)))
            .build()

        val workMgr = WorkManager.getInstance(context)
        val observer = object : Observer<WorkInfo?> {
            override fun onChanged(value: WorkInfo?) {
                if (value == null) {
                    // Handle the case where workInfo is null
                    return
                }

                if (value.state == WorkInfo.State.SUCCEEDED
                    || value.state == WorkInfo.State.FAILED
                ) {

                    val errStringId: StringResource
                    var errRemedyIntent: String? = null
                    if (value.state == WorkInfo.State.FAILED) {
                        var errId =
                            value.outputData.getString(BackgroundSpeechService.FAILURE_REASON)
                        when (errId) {
                            BackgroundSpeechService.FAILURE_MUTED
                                -> errStringId = Res.string.speech_failure_toast_muted

                            BackgroundSpeechService.FAILURE_INIT_FAILED -> {
                                errStringId = Res.string.speech_failure_toast_init
                                errRemedyIntent = Settings.ACTION_ACCESSIBILITY_SETTINGS
                            }

                            BackgroundSpeechService.FAILURE_LANG_UNSUPPORTED -> {
                                errStringId = Res.string.speech_failure_toast_chinese_unsupported
                                errRemedyIntent = TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA
                            }

                            else -> {
                                errStringId = Res.string.speech_failure_toast_unknown
                                errId = BackgroundSpeechService.FAILURE_UNKNOWN
                            }
                        }

                        logAnalyticsError("SPEECH", errId, "")

                        CoroutineScope(Dispatchers.IO).launch {
                            val errString = getString(errStringId)
                            val titleString =getString(Res.string.dialog_tts_error)
                            val yesButton = getString(Res.string.fix_it)
                            val noButton = getString(Res.string.cancel)

                            if (errRemedyIntent == null) {
                                toast(errStringId)
                            } else {
                                withContext(Dispatchers.Main) {
                                    AlertDialog.Builder(context)
                                        .setTitle(titleString)
                                        .setMessage(errString)
                                        .setPositiveButton(yesButton) { _, _ ->
                                            val intent = Intent(errRemedyIntent)
                                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                            context.startActivity(intent)
                                        }
                                        .setNegativeButton(noButton, null)
                                        .show()
                                }
                            }
                        }
                    }

                    workMgr.getWorkInfoByIdLiveData(speechRequest.id)
                        .removeObserver(this)
                }
            }
        }

        workMgr.getWorkInfoByIdLiveData(speechRequest.id).observeForever(observer)

        workMgr.enqueue(speechRequest)

        incrementConsultedWord(word)

        logAnalyticsEvent(Utils.ANALYTICS_EVENTS.WIDGET_PLAY_WORD)
    }

    actual fun toast(s: String) {
        CoroutineScope(Dispatchers.Main).launch {
            Toast.makeText(context, s, Toast.LENGTH_LONG).show()
        }
    }

    actual fun openAppForSearchQuery(query: SearchQuery) {
        val context = context
        val pm = context.packageManager
        val launchIntent = pm.getLaunchIntentForPackage(context.packageName)?.apply {
            putExtra(INTENT_SEARCH_WORD, query.toString())
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }

        if (launchIntent != null) {
            Utils.logAnalyticsEvent(Utils.ANALYTICS_EVENTS.WIDGET_OPEN_DICTIONARY)
            context.startActivity(launchIntent)
        } else {
            // fallback: app has no launch intent?
            Logger.e(tag = TAG, messageString = "No launch intent found for ${context.packageName}")
        }
    }

    actual suspend fun copyFileSafely(sourceFile: PlatformFile, destinationDir: BookmarkData, filename: String) {
        withContext(Dispatchers.IO) {
            val context = context
            // Open input stream for the source database file
            val inputStream: InputStream = FileInputStream(File(sourceFile.path))

            // Convert BookmarkData -> PlatformFile -> Uri
            val folderPF = PlatformFile.fromBookmarkData(destinationDir)
            val folderUri = (folderPF.androidFile as? AndroidFile.UriWrapper)?.uri
                ?: throw IllegalArgumentException("BookmarkData must point to a Uri folder")

            val dir = DocumentFile.fromTreeUri(context, folderUri)
                ?: throw IllegalStateException("Cannot access folder DocumentFile")

            val destinationFile = dir.createFile("application/octet-stream", filename)
                ?: throw IllegalStateException("Could not create file in destination folder")

            // Open OutputStream to the destination file
            context.contentResolver.openFileDescriptor(destinationFile.uri, "w")
                ?.use { parcelFileDescriptor ->
                    FileOutputStream(parcelFileDescriptor.fileDescriptor).use { output ->
                        // Copy data from source to destination
                        inputStream.use { input ->
                            val buffer = ByteArray(1024)
                            var length: Int
                            while (input.read(buffer).also { length = it } > 0) {
                                output.write(buffer, 0, length)
                            }
                        }
                    }
                }
        }
    }

    private const val TAG = "Utils"
    const val INTENT_SEARCH_WORD = "INTENT_SEARCH_WORD"
}

actual fun PlatformFile.createdAt(): Instant? {
    return this.androidFile.let { androidFile ->
        when (androidFile) {
            is AndroidFile.FileWrapper -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val attributes = Files.readAttributes(
                        androidFile.file.toPath(),
                        BasicFileAttributes::class.java
                    )
                    val timestamp = attributes.creationTime().toMillis()
                    Instant.fromEpochMilliseconds(timestamp)
                } else {
                    // Fallback for older Android versions
                    null
                }
            }

            is AndroidFile.UriWrapper -> null
        }
    }
}

actual fun PlatformFile.lastModified(): Instant {
    val timestamp = this.androidFile.let { androidFile ->
        when (androidFile) {
            is AndroidFile.FileWrapper -> androidFile.file.lastModified()
            is AndroidFile.UriWrapper -> DocumentFile
                .fromSingleUri(FileKit.context, androidFile.uri)
                ?.lastModified()
                ?: throw IllegalStateException("Unable to get last modified date for URI")
        }
    }

    return Instant.fromEpochMilliseconds(timestamp)
}